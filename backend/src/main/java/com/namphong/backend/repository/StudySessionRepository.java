package com.namphong.backend.repository;

import com.namphong.backend.entity.SessionStatus;
import com.namphong.backend.entity.StudySession;
import com.namphong.backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface StudySessionRepository extends JpaRepository<StudySession, UUID> {
    List<StudySession> findByUserAndStatusAndStartTimeBetween(
            UserEntity user, SessionStatus status, LocalDateTime start, LocalDateTime end);

    List<StudySession> findByUserAndStartTimeBetween(
            UserEntity user, LocalDateTime start, LocalDateTime end);

    // find if the user has the lasted active study session
    Optional<StudySession> findFirstByUserIdAndStatusOrderByStartTimeDesc(UUID userId, SessionStatus status);

    /** Name of the partial unique index from V3: at most one RUNNING session per user. */
    String ONE_RUNNING_SESSION_INDEX = "uq_study_sessions_one_running_per_user";

    /**
     * Completes the session only if it is still RUNNING, as one atomic statement.
     * Returns 1 for the request that won, 0 for every concurrent or repeated request.
     *
     * A read-then-write ("load, check status, save") lets two requests both see RUNNING
     * and both credit the time. Here PostgreSQL row-locks the row: the second UPDATE waits,
     * re-checks the WHERE on the committed row, and matches nothing.
     *
     * clearAutomatically: the bulk update bypasses the persistence context, so drop
     * the now-stale RUNNING copy instead of letting Hibernate flush it back later.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update StudySession s set s.status = :completed, s.endTime = :endTime, s.durationSeconds = :duration " +
           "where s.id = :id and s.status = :running")
    int completeIfRunning(@Param("id") UUID id,
                          @Param("endTime") LocalDateTime endTime,
                          @Param("duration") int duration,
                          @Param("running") SessionStatus running,
                          @Param("completed") SessionStatus completed);

    default int completeIfRunning(UUID id, LocalDateTime endTime, int duration) {
        return completeIfRunning(id, endTime, duration, SessionStatus.RUNNING, SessionStatus.COMPLETED);
    }

    // ---- batched lookups for a whole group (avoid one query per member) ----

    interface UserStartTime {
        UUID getUserId();
        LocalDateTime getStartTime();
    }

    interface UserDuration {
        UUID getUserId();
        Long getTotalSeconds();
    }

    @Query("select s.user.id as userId, s.startTime as startTime from StudySession s " +
           "where s.user.id in :userIds and s.status = :status")
    List<UserStartTime> findStartTimesByUserIdsAndStatus(@Param("userIds") Collection<UUID> userIds,
                                                         @Param("status") SessionStatus status);

    @Query("select s.user.id as userId, coalesce(sum(s.durationSeconds), 0) as totalSeconds from StudySession s " +
           "where s.user.id in :userIds and s.status = :status and s.startTime >= :from and s.startTime < :to " +
           "group by s.user.id")
    List<UserDuration> sumDurationByUserIds(@Param("userIds") Collection<UUID> userIds,
                                            @Param("status") SessionStatus status,
                                            @Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);

    /** userId -> start time of that user's latest RUNNING session (users without one are absent). */
    default Map<UUID, LocalDateTime> findLatestRunningStartTimes(Collection<UUID> userIds) {
        Map<UUID, LocalDateTime> latest = new HashMap<>();
        if (userIds.isEmpty()) {
            return latest;
        }
        for (UserStartTime row : findStartTimesByUserIdsAndStatus(userIds, SessionStatus.RUNNING)) {
            if (row.getStartTime() != null) {
                latest.merge(row.getUserId(), row.getStartTime(), (a, b) -> a.isAfter(b) ? a : b);
            }
        }
        return latest;
    }

    // ---- goals, achievements, challenges ----

    interface SessionSlice {
        UUID getId();
        LocalDateTime getStartTime();
        Integer getDurationSeconds();
    }

    /** The user's completed sessions that started in [from, to): only the columns progress needs. */
    @Query("select s.id as id, s.startTime as startTime, s.durationSeconds as durationSeconds from StudySession s " +
           "where s.user.id = :userId and s.status = :status and s.startTime >= :from and s.startTime < :to")
    List<SessionSlice> findSlices(@Param("userId") UUID userId,
                                  @Param("status") SessionStatus status,
                                  @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to);

    default List<SessionSlice> findCompletedSlices(UUID userId, LocalDateTime from, LocalDateTime to) {
        return findSlices(userId, SessionStatus.COMPLETED, from, to);
    }

    long countByUserIdAndStatus(UUID userId, SessionStatus status);

    @Query("select coalesce(sum(s.durationSeconds), 0) from StudySession s where s.user.id = :userId and s.status = :status")
    long sumDurationByUserId(@Param("userId") UUID userId, @Param("status") SessionStatus status);

    /** Days (server time) with at least one credited session, newest first: input for streaks. */
    @Query(value = "select distinct cast(start_time as date) as study_day from study_sessions " +
                   "where user_id = :userId and status = 'COMPLETED' and coalesce(duration_seconds, 0) > 0 " +
                   "order by study_day desc",
           nativeQuery = true)
    List<Object> findStudyDaysRaw(@Param("userId") UUID userId);

    default List<java.time.LocalDate> findStudyDays(UUID userId) {
        return findStudyDaysRaw(userId).stream()
                .map(StudySessionRepository::toLocalDate)
                .toList();
    }

    private static java.time.LocalDate toLocalDate(Object value) {
        if (value instanceof java.time.LocalDate date) {
            return date;
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        if (value instanceof java.time.LocalDateTime dateTime) {
            return dateTime.toLocalDate();
        }
        return java.time.LocalDate.parse(String.valueOf(value));
    }
}
