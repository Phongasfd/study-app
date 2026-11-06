package com.namphong.backend.service;

import com.namphong.backend.entity.GroupMember;
import com.namphong.backend.entity.StudyGroup;
import com.namphong.backend.entity.StudySession;
import com.namphong.backend.entity.UserEntity;
import com.namphong.backend.repository.GroupMemberRepository;
import com.namphong.backend.repository.GroupRepository;
import com.namphong.backend.repository.UserRepository;
import com.namphong.backend.support.EmbeddedPostgresSupport;
import com.namphong.backend.websocket.GroupEventBus;
import com.namphong.backend.websocket.RankingBroadcaster;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * More members press start at the same moment than the pool has connections.
 *
 * The ranking push used to read the board in the after-commit callback, on the request thread,
 * while the committed transaction still held its connection, so every request needed a second
 * one. With more simultaneous requests than connections each thread held one and waited for
 * another until the pool timed out, and the pushes of the whole burst were lost (about ten
 * members pressing start together on the default pool of 10).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED) // real commits, like production
@Import({StudySessionService.class, GroupRankingService.class, RankingBroadcaster.class,
        RankingBroadcastPoolTest.FixedClock.class})
class RankingBroadcastPoolTest {

    private static final int POOL_SIZE = 4;
    private static final int MEMBERS = 12;
    private static final long POOL_TIMEOUT_MS = 3_000;

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-11-05T10:00:00Z"), ZoneOffset.UTC);
        }
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> EmbeddedPostgresSupport.freshDatabaseUrl("broadcastpool"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> String.valueOf(POOL_SIZE));
        registry.add("spring.datasource.hikari.connection-timeout", () -> String.valueOf(POOL_TIMEOUT_MS));
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired private StudySessionService service;
    @Autowired private UserRepository users;
    @Autowired private GroupRepository groups;
    @Autowired private GroupMemberRepository members;
    @MockitoBean private GroupEventBus groupEventBus;

    @Test
    void aBurstOfStartsLargerThanThePoolPushesEveryRankingWithoutStalling() throws Exception {
        StudyGroup group = new StudyGroup();
        group.setName("burst");
        group.setOwner(newUser());
        group.setMaxMembers(MEMBERS + 1);
        StudyGroup savedGroup = groups.save(group);
        List<UserEntity> people = new ArrayList<>();
        for (int i = 0; i < MEMBERS; i++) {
            UserEntity person = newUser();
            GroupMember member = new GroupMember();
            member.setGroup(savedGroup);
            member.setUser(person);
            member.setJoinedAt(LocalDateTime.of(2026, 11, 5, 9, 0));
            members.save(member);
            people.add(person);
        }

        ExecutorService requests = Executors.newFixedThreadPool(MEMBERS);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<StudySession>> starts = new ArrayList<>();
        for (UserEntity person : people) {
            starts.add(requests.submit(() -> {
                go.await();
                return service.createStudySession(person);
            }));
        }
        long startedAt = System.nanoTime();
        go.countDown();
        for (Future<StudySession> start : starts) {
            assertNotNull(start.get(30, TimeUnit.SECONDS));
        }
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        requests.shutdown();

        // the old code made every thread wait for a second connection until the pool timed out
        assertTrue(elapsedMs < POOL_TIMEOUT_MS, "the burst stalled for " + elapsedMs + " ms");
        verify(groupEventBus, timeout(10_000).times(MEMBERS)).publish(eq(savedGroup.getId()), eq("ranking"), any());
    }

    private UserEntity newUser() {
        String name = "u" + UUID.randomUUID().toString().substring(0, 8);
        return users.save(UserEntity.builder().username(name).email(name + "@example.com").build());
    }
}
