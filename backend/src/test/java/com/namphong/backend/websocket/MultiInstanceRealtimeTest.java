package com.namphong.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.namphong.backend.config.InMemoryRateLimiter;
import com.namphong.backend.config.RateLimiter;
import com.namphong.backend.config.RateLimiterProperties;
import com.namphong.backend.config.RedisRateLimiter;
import com.namphong.backend.config.ScalingConfig;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Two backend "nodes" in one JVM, each with its own WebSocket registry, Redis connection,
 * Pub/Sub listener and rate limiter, sharing one real Redis server (embedded redis-server
 * binary). This is the situation behind a load balancer: members of the same group are
 * connected to different instances.
 */
class MultiInstanceRealtimeTest {

    private static RedisServer redisServer;
    private static int redisPort;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<Node> nodes = new ArrayList<>();
    private final UUID groupId = UUID.randomUUID();

    /** One backend instance: the beans ScalingConfig.MultiInstance would create. */
    private final class Node implements AutoCloseable {
        final GroupSessionRegistry registry = new GroupSessionRegistry();
        final LettuceConnectionFactory connectionFactory;
        final StringRedisTemplate redis;
        final RedisGroupEventBus bus;
        final ExecutorService dispatcher = RedisGroupEventBus.singleThreadDispatcher();
        final RedisMessageListenerContainer listener;
        final RateLimiter rateLimiter;

        Node(int port, RateLimiterProperties limits) {
            connectionFactory = connectionFactory(port);
            redis = new StringRedisTemplate(connectionFactory);
            bus = new RedisGroupEventBus(redis, registry, objectMapper);
            listener = RedisGroupEventBus.listenerContainer(connectionFactory, bus, dispatcher); // same as ScalingConfig
            listener.afterPropertiesSet();
            listener.start();
            rateLimiter = new RedisRateLimiter(redis, limits);
        }

        WebSocketSession connect(UUID group) {
            WebSocketSession session = mock(WebSocketSession.class);
            when(session.isOpen()).thenReturn(true);
            when(session.getId()).thenReturn(UUID.randomUUID().toString());
            registry.add(group, session);
            return session;
        }

        @Override
        public void close() throws Exception {
            listener.stop();
            listener.destroy();
            dispatcher.shutdownNow();
            connectionFactory.destroy();
        }
    }

    private static LettuceConnectionFactory connectionFactory(int port) {
        LettuceClientConfiguration client = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(2))
                .clientOptions(ClientOptions.builder()
                        .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(1)).build())
                        .build())
                .build();
        LettuceConnectionFactory factory = new LettuceConnectionFactory(new RedisStandaloneConfiguration("127.0.0.1", port), client);
        factory.afterPropertiesSet();
        factory.start();
        return factory;
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    @BeforeAll
    static void startRedis() throws IOException {
        redisPort = freePort();
        redisServer = RedisServer.newRedisServer().port(redisPort).bind("127.0.0.1").build();
        redisServer.start();
    }

    @AfterAll
    static void stopRedis() throws IOException {
        if (redisServer != null) {
            redisServer.stop();
        }
    }

    @BeforeEach
    void startTwoNodes() throws Exception {
        RateLimiterProperties limits = new RateLimiterProperties();
        limits.setRequests(3);
        limits.setDurationSeconds(60);
        nodes.add(new Node(redisPort, limits));
        nodes.add(new Node(redisPort, limits));
        awaitSubscribed();
    }

    @AfterEach
    void stopNodes() throws Exception {
        for (Node node : nodes) {
            node.close();
        }
    }

    private Node nodeA() {
        return nodes.get(0);
    }

    private Node nodeB() {
        return nodes.get(1);
    }

    /** Pub/Sub drops messages sent before a node has subscribed; probe until every node hears. */
    private void awaitSubscribed() throws InterruptedException {
        UUID probeGroup = UUID.randomUUID();
        List<WebSocketSession> probes = nodes.stream().map(n -> n.connect(probeGroup)).toList();
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            nodeA().bus.publish(probeGroup, "probe", Map.of());
            Thread.sleep(100);
            if (probes.stream().allMatch(p -> !mockingDetails(p).getInvocations().stream()
                    .filter(i -> i.getMethod().getName().equals("sendMessage")).toList().isEmpty())) {
                return;
            }
        }
        fail("nodes did not subscribe to Redis within 10 seconds");
    }

    private static TextMessage messageContaining(String text) {
        return argThat(m -> m instanceof TextMessage t && t.getPayload().contains(text));
    }

    @Test
    void chatSentOnNodeA_reachesMembersOnBothNodes_exactlyOnce() throws Exception {
        WebSocketSession memberOnA = nodeA().connect(groupId);
        WebSocketSession memberOnB = nodeB().connect(groupId);

        nodeA().bus.publish(groupId, "chat", Map.of("content", "hello from A"));

        verify(memberOnB, timeout(5_000)).sendMessage(messageContaining("hello from A"));
        verify(memberOnA, timeout(5_000)).sendMessage(messageContaining("hello from A"));
        // node A gets its own message back from Redis and must not also deliver it directly
        verify(memberOnA, after(300).times(1)).sendMessage(any());
        verify(memberOnB, times(1)).sendMessage(any());
    }

    @Test
    void messagesArriveInTheOrderTheyWereSent() throws Exception {
        List<String> received = new CopyOnWriteArrayList<>();
        WebSocketSession memberOnB = mock(WebSocketSession.class);
        when(memberOnB.isOpen()).thenReturn(true);
        when(memberOnB.getId()).thenReturn("b");
        doAnswer(inv -> received.add(((TextMessage) inv.getArgument(0)).getPayload()))
                .when(memberOnB).sendMessage(any());
        nodeB().registry.add(groupId, memberOnB);

        int count = 200;
        for (int i = 0; i < count; i++) {
            nodeA().bus.publish(groupId, "chat", Map.of("seq", i));
        }

        long deadline = System.currentTimeMillis() + 10_000;
        while (received.size() < count && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        List<Integer> order = new ArrayList<>();
        for (String payload : received) {
            order.add(objectMapper.readTree(payload).get("data").get("seq").asInt());
        }
        // a chat must not show "ok, see you" before "shall we start?"
        assertEquals(IntStream.range(0, count).boxed().toList(), order);
    }

    @Test
    void envelopeKeepsTheFormatTheFrontendParses() throws Exception {
        WebSocketSession memberOnB = nodeB().connect(groupId);

        nodeA().bus.publish(groupId, "ranking", List.of(Map.of("rank", 1)));

        verify(memberOnB, timeout(5_000)).sendMessage(messageContaining("{\"type\":\"ranking\",\"data\":[{\"rank\":1}]}"));
    }

    @Test
    void membersOfOtherGroupsReceiveNothing() throws Exception {
        WebSocketSession otherGroupOnB = nodeB().connect(UUID.randomUUID());

        nodeA().bus.publish(groupId, "chat", Map.of("content", "only for group G"));

        verify(otherGroupOnB, after(500).never()).sendMessage(any());
    }

    @Test
    void withoutRedis_aSecondNodeNeverSeesTheMessage() throws Exception {
        // The single-instance bus behind a load balancer: why Redis is needed at all
        WebSocketSession memberOnB = nodeB().connect(groupId);
        LocalGroupEventBus localBusOfA = new LocalGroupEventBus(nodeA().registry, objectMapper);

        localBusOfA.publish(groupId, "chat", Map.of("content", "lost"));

        verify(memberOnB, after(500).never()).sendMessage(any());
    }

    @Test
    void rateLimitIsSharedAcrossNodes() {
        String client = "203.0.113." + (int) (Math.random() * 250);

        assertTrue(nodeA().rateLimiter.tryAcquire(client));
        assertTrue(nodeA().rateLimiter.tryAcquire(client));
        assertTrue(nodeB().rateLimiter.tryAcquire(client));
        assertFalse(nodeB().rateLimiter.tryAcquire(client), "4th request is over the limit of 3, on whichever node");
        assertFalse(nodeA().rateLimiter.tryAcquire(client));
        assertTrue(nodeB().rateLimiter.tryAcquire(client + "-other"), "other clients are unaffected");
    }

    @Test
    void inMemoryLimiters_wouldAllowTheLimitOncePerNode() {
        // Contrast: per-JVM buckets behind a load balancer let a client through N x limit
        RateLimiterProperties limits = new RateLimiterProperties();
        limits.setRequests(3);
        limits.setDurationSeconds(60);
        RateLimiter a = new InMemoryRateLimiter(limits);
        RateLimiter b = new InMemoryRateLimiter(limits);

        int allowed = 0;
        for (int i = 0; i < 3; i++) {
            allowed += a.tryAcquire("c") ? 1 : 0;
            allowed += b.tryAcquire("c") ? 1 : 0;
        }
        assertEquals(6, allowed);
    }

    // ---- Spring wiring: app.redis.enabled picks the implementations (ScalingConfig) ----

    private ApplicationContextRunner scalingContext() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DataRedisAutoConfiguration.class))
                .withUserConfiguration(ScalingConfig.class)
                .withBean(GroupSessionRegistry.class)
                .withBean(ObjectMapper.class)
                .withBean(RateLimiterProperties.class)
                .withPropertyValues("spring.data.redis.host=127.0.0.1", "spring.data.redis.port=" + redisPort);
    }

    @Test
    void byDefault_theAppRunsAsASingleInstance() {
        scalingContext().run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(GroupEventBus.class)).isInstanceOf(LocalGroupEventBus.class);
            assertThat(context.getBean(RateLimiter.class)).isInstanceOf(InMemoryRateLimiter.class);
            assertThat(context).doesNotHaveBean(RedisMessageListenerContainer.class);
        });
    }

    @Test
    void redisEnabled_switchesFanOutAndRateLimitToRedis() {
        scalingContext().withPropertyValues("app.redis.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(GroupEventBus.class)).isInstanceOf(RedisGroupEventBus.class);
            assertThat(context.getBean(RateLimiter.class)).isInstanceOf(RedisRateLimiter.class);
            assertThat(context).hasSingleBean(RedisMessageListenerContainer.class);
        });
    }

    @Test
    void redisDown_eventsStillReachLocalSocketsAndRequestsAreNotBlocked() throws Exception {
        LettuceConnectionFactory deadRedis = connectionFactory(freePort()); // nothing listens there
        try {
            StringRedisTemplate redis = new StringRedisTemplate(deadRedis);
            GroupSessionRegistry registry = new GroupSessionRegistry();
            WebSocketSession local = mock(WebSocketSession.class);
            when(local.isOpen()).thenReturn(true);
            when(local.getId()).thenReturn("local");
            registry.add(groupId, local);
            RateLimiterProperties limits = new RateLimiterProperties();
            limits.setRequests(1);

            new RedisGroupEventBus(redis, registry, objectMapper).publish(groupId, "chat", Map.of("content", "x"));
            RateLimiter limiter = new RedisRateLimiter(redis, limits);

            verify(local).sendMessage(any()); // degraded to single-node delivery
            assertTrue(limiter.tryAcquire("c") && limiter.tryAcquire("c"), "fails open");
        } finally {
            deadRedis.destroy();
        }
    }
}
