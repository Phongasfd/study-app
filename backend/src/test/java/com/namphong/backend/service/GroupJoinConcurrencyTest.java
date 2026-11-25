package com.namphong.backend.service;

import com.namphong.backend.dto.JoinResultResponse;
import com.namphong.backend.entity.*;
import com.namphong.backend.exception.BadRequestException;
import com.namphong.backend.repository.*;
import com.namphong.backend.support.EmbeddedPostgresSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Joining groups under concurrency, on a real PostgreSQL 16. Every call commits on its own.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({GroupMemberService.class, GroupJoinService.class, NotificationService.class,
        GroupJoinConcurrencyTest.FixedClock.class})
class GroupJoinConcurrencyTest {

    private static final int THREADS = 8;

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-11-05T10:00:00Z"), ZoneOffset.UTC);
        }
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> EmbeddedPostgresSupport.freshDatabaseUrl("joins"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> String.valueOf(THREADS * 2));
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired private GroupJoinService joinService;
    @Autowired private GroupMemberService memberService;
    @Autowired private UserRepository users;
    @Autowired private GroupRepository groups;
    @Autowired private GroupMemberRepository members;
    @Autowired private GroupJoinRequestRepository requests;
    @Autowired private NotificationRepository notifications;

    private UserEntity owner;

    @BeforeEach
    void setUp() {
        owner = newUser();
    }

    private UserEntity newUser() {
        String name = "u" + UUID.randomUUID().toString().substring(0, 8);
        return users.save(UserEntity.builder().username(name).email(name + "@example.com").build());
    }

    private StudyGroup group(GroupVisibility visibility, int maxMembers) {
        StudyGroup group = new StudyGroup();
        group.setName("g-" + UUID.randomUUID().toString().substring(0, 6));
        group.setOwner(owner);
        group.setMaxMembers(maxMembers);
        group.setVisibility(visibility);
        group.setCreatedAt(LocalDateTime.now());
        group = groups.save(group);
        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(owner);
        member.setJoinedAt(LocalDateTime.now());
        members.save(member);
        return group;
    }

    private long notificationsOf(UserEntity user, NotificationType type) {
        return notifications.findAll().stream()
                .filter(n -> n.getUser().getId().equals(user.getId()) && n.getType() == type)
                .count();
    }

    @Test
    void concurrentJoinsNeverExceedTheCapacity() throws Exception {
        StudyGroup group = group(GroupVisibility.PUBLIC, 4); // owner + 3 free seats
        List<UserEntity> joiners = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            joiners.add(newUser());
        }
        AtomicInteger next = new AtomicInteger();

        // without the row lock all 8 see 1 member and get in
        List<Boolean> joined = runConcurrently(() -> {
            UserEntity user = joiners.get(next.getAndIncrement());
            try {
                return JoinResultResponse.JOINED.equals(joinService.join(group.getId(), user.getId(), null).status());
            } catch (BadRequestException full) {
                return false;
            }
        });

        assertEquals(3, joined.stream().filter(b -> b).count(), "exactly the free seats are taken");
        assertEquals(4, members.countByGroupId(group.getId()));
    }

    @Test
    void privateGroupRequestIsApprovedExactlyOnce() throws Exception {
        StudyGroup group = group(GroupVisibility.PRIVATE, 10);
        UserEntity requester = newUser();

        JoinResultResponse result = joinService.join(group.getId(), requester.getId(), "let me in");
        assertEquals(JoinResultResponse.REQUESTED, result.status());
        assertFalse(memberService.isMember(group.getId(), requester.getId()), "not a member before approval");
        assertEquals(result.requestId(), joinService.join(group.getId(), requester.getId(), null).requestId(),
                "asking twice returns the same open request");
        assertEquals(1, notificationsOf(owner, NotificationType.JOIN_REQUEST_RECEIVED));

        // 8 clicks on "approve" at once (two tabs, double clicks)
        List<Boolean> approved = runConcurrently(() -> {
            try {
                joinService.approve(group.getId(), result.requestId(), owner.getId());
                return true;
            } catch (BadRequestException alreadyHandled) {
                return false;
            }
        });

        assertEquals(1, approved.stream().filter(b -> b).count(), "one approval wins");
        assertTrue(memberService.isMember(group.getId(), requester.getId()));
        assertEquals(2, members.countByGroupId(group.getId()));
        assertEquals(1, notificationsOf(requester, NotificationType.JOIN_REQUEST_APPROVED));
        assertEquals(JoinRequestStatus.APPROVED, requests.findById(result.requestId()).orElseThrow().getStatus());
    }

    @Test
    void approvingWhenTheGroupFilledUpRollsBackAndKeepsTheRequestPending() {
        StudyGroup group = group(GroupVisibility.PRIVATE, 2);
        UserEntity requester = newUser();
        UUID requestId = joinService.join(group.getId(), requester.getId(), null).requestId();

        // the last seat goes to someone the owner added directly
        memberService.addMemberAsOwner(group.getId(), newUser().getId(), owner.getId());

        assertThrows(BadRequestException.class, () -> joinService.approve(group.getId(), requestId, owner.getId()));
        assertEquals(JoinRequestStatus.PENDING, requests.findById(requestId).orElseThrow().getStatus(),
                "the status change was rolled back with the failed insert");
        assertEquals(0, notificationsOf(requester, NotificationType.JOIN_REQUEST_APPROVED));
    }

    @Test
    void onlyTheOwnerDecidesAndRequestersCanWithdraw() {
        StudyGroup group = group(GroupVisibility.PRIVATE, 10);
        UserEntity requester = newUser();
        UUID requestId = joinService.join(group.getId(), requester.getId(), null).requestId();

        assertThrows(AccessDeniedException.class, () -> joinService.approve(group.getId(), requestId, requester.getId()));

        joinService.cancel(group.getId(), requester.getId());
        assertEquals(JoinRequestStatus.CANCELLED, requests.findById(requestId).orElseThrow().getStatus());
        assertThrows(BadRequestException.class, () -> joinService.approve(group.getId(), requestId, owner.getId()),
                "a withdrawn request can't be approved");
        assertTrue(joinService.pendingForUser(requester.getId()).isEmpty());
    }

    private static <T> List<T> runConcurrently(Callable<T> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                return task.call();
            }));
        }
        ready.await();
        go.countDown();
        List<T> results = new ArrayList<>();
        for (Future<T> f : futures) {
            results.add(f.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();
        return results;
    }
}
