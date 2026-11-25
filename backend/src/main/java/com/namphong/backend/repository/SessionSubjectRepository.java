package com.namphong.backend.repository;

import com.namphong.backend.entity.SessionSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SessionSubjectRepository extends JpaRepository<SessionSubject, UUID> {
    List<SessionSubject> findBySessionId(UUID sessionId);
    List<SessionSubject> findBySessionIdIn(List<UUID> sessionIds);

    interface SessionSubjectLink {
        UUID getSessionId();
        UUID getSubjectId();
    }

    // ids only: the derived findBySessionIdIn loads the EAGER session and subject of every row
    @Query("select ss.session.id as sessionId, ss.subject.id as subjectId from SessionSubject ss " +
           "where ss.session.id in :sessionIds")
    List<SessionSubjectLink> findLinksBySessionIds(@Param("sessionIds") Collection<UUID> sessionIds);
}
