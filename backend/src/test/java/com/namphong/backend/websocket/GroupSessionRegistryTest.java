package com.namphong.backend.websocket;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupSessionRegistryTest {

    /**
     * ConcurrentWebSocketSessionDecorator only flags a session that exceeds its limits and
     * throws; it never closes it. The registry used to log that and keep the session, so the
     * client stayed connected but received nothing more and never reconnected.
     */
    @Test
    void aClientThatStopsReadingIsClosedAndDroppedInsteadOfLeftMuted() throws Exception {
        GroupSessionRegistry registry = new GroupSessionRegistry();
        UUID groupId = UUID.randomUUID();
        CountDownLatch writeBlocked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        WebSocketSession slow = mock(WebSocketSession.class);
        when(slow.isOpen()).thenReturn(true);
        when(slow.getId()).thenReturn("slow");
        doAnswer(invocation -> {
            writeBlocked.countDown();
            release.await(10, TimeUnit.SECONDS); // the client stopped reading: the write hangs
            return null;
        }).when(slow).sendMessage(any());
        registry.add(groupId, slow);

        Thread firstDelivery = new Thread(() -> registry.deliverLocally(groupId, "{\"type\":\"chat\"}"));
        firstDelivery.start();
        assertTrue(writeBlocked.await(5, TimeUnit.SECONDS));

        // more than the buffer limit queues up behind the stuck write
        registry.deliverLocally(groupId, "x".repeat(GroupSessionRegistry.BUFFER_SIZE_LIMIT_BYTES + 1));

        verify(slow, timeout(2_000)).close(any(CloseStatus.class)); // the client sees the close and reconnects
        assertEquals(0, registry.localSessionCount(groupId), "no more deliveries to a dead socket");

        release.countDown();
        firstDelivery.join(5_000);
    }
}
