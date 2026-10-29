package com.namphong.backend.service;

import com.namphong.backend.dto.GroupRankingResponse;
import com.namphong.backend.entity.*;
import com.namphong.backend.support.EmbeddedPostgresSupport;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs the ranking board against a real PostgreSQL (schema from the Flyway migrations)
 * and counts the SQL statements Hibernate actually sends.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(GroupRankingService.class)
class GroupRankingQueryCountTest {

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> EmbeddedPostgresSupport.freshDatabaseUrl("ranking"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
    }

    @Autowired
    private TestEntityManager em;

    @Autowired
    private GroupRankingService groupRankingService;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        statistics = em.getEntityManager().getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
    }

    private UserEntity user(String name) {
        return em.persist(UserEntity.builder().username(name).email(name + "@example.com").build());
    }

    private StudyGroup groupWithMembers(int memberCount) {
        UserEntity owner = user("owner" + UUID.randomUUID().toString().substring(0, 8));
        StudyGroup group = new StudyGroup();
        group.setName("g");
        group.setOwner(owner);
        group.setMaxMembers(50);
        em.persist(group);

        for (int i = 0; i < memberCount; i++) {
            UserEntity u = i == 0 ? owner : user("u" + UUID.randomUUID().toString().substring(0, 8));
            GroupMember member = new GroupMember();
            member.setGroup(group);
            member.setUser(u);
            member.setJoinedAt(LocalDateTime.now());
            em.persist(member);
            em.persist(GroupRanking.builder().group(group).user(u).totalDuration(100 * (i + 1)).build());
            // everyone studied 10 minutes earlier today; every other member is studying right now
            em.persist(StudySession.builder().user(u).startTime(LocalDateTime.now().minusMinutes(30))
                    .endTime(LocalDateTime.now().minusMinutes(20)).durationSeconds(600)
                    .status(SessionStatus.COMPLETED).build());
            if (i % 2 == 0) {
                em.persist(StudySession.builder().user(u).startTime(LocalDateTime.now().minusMinutes(5))
                        .status(SessionStatus.RUNNING).build());
            }
        }
        em.flush();
        em.clear(); // nothing cached: every row must come from SQL
        return group;
    }

    private long statementsFor(UUID groupId) {
        statistics.clear();
        List<GroupRankingResponse> board = groupRankingService.findGroupRankingById(groupId);
        long count = statistics.getPrepareStatementCount();
        assertFalse(board.isEmpty());
        em.clear();
        return count;
    }

    @Test
    void queryCountDoesNotGrowWithGroupSize() {
        long small = statementsFor(groupWithMembers(3).getId());
        long large = statementsFor(groupWithMembers(15).getId());

        assertEquals(small, large, "ranking must not issue a query per member (N+1)");
        assertTrue(large <= 4, "expected 4 statements, got " + large);
    }

    @Test
    void computesTheSameNumbersAsBefore() {
        StudyGroup group = groupWithMembers(3);

        List<GroupRankingResponse> board = groupRankingService.findGroupRankingById(group.getId());

        assertEquals(3, board.size());
        GroupRankingResponse first = board.get(0);
        // member #3 (index 2): ranking 300s + ~5 min running session, studying now
        assertTrue(first.getIsStudying());
        assertTrue(first.getTotalDuration() >= 300 + 299, "db total + running time");
        assertEquals(600, first.getBaseDuration(), "today's completed seconds");
        assertEquals(1, first.getRank());
        // member #2 is not studying: total is just the stored ranking
        GroupRankingResponse notStudying = board.stream().filter(r -> !r.getIsStudying()).findFirst().orElseThrow();
        assertEquals(200, notStudying.getTotalDuration());
        assertNull(notStudying.getActiveSessionStartTime());
    }

    @Test
    void emptyGroup() {
        StudyGroup group = new StudyGroup();
        group.setName("empty");
        em.persist(group);
        em.flush();

        assertTrue(groupRankingService.findGroupRankingById(group.getId()).isEmpty());
    }
}
