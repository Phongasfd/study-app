package com.namphong.backend.service;

import com.namphong.backend.dto.WeeklySubjectStatsResponse;
import com.namphong.backend.entity.SessionSubject;
import com.namphong.backend.entity.StudySession;
import com.namphong.backend.entity.Subject;
import com.namphong.backend.entity.UserEntity;
import com.namphong.backend.repository.SessionSubjectRepository;
import com.namphong.backend.repository.StudySessionRepository;
import com.namphong.backend.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SessionSubjectService {
    private final SessionSubjectRepository sessionSubjectRepository;
    private final StudySessionRepository studySessionRepository;
    private final SubjectRepository subjectRepository;

    public SessionSubject addSubjectToSession(UUID sessionId, UUID subjectId) {
        StudySession session = studySessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        SessionSubject sessionSubject = new SessionSubject();
        sessionSubject.setSession(session);
        sessionSubject.setSubject(subject);

        return sessionSubjectRepository.save(sessionSubject);
    }

    public List<SessionSubject> getSubjectsBySession(UUID sessionId) {
        return sessionSubjectRepository.findBySessionId(sessionId);
    }

    public List<WeeklySubjectStatsResponse> getWeeklySubjectStats(UserEntity user) {
        // Determine current week's Monday (start) and Sunday (end)
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate sunday = today.with(DayOfWeek.SUNDAY);

        LocalDateTime weekStart = monday.atStartOfDay();
        LocalDateTime weekEnd = sunday.atTime(LocalTime.MAX);

        // Fetch sessions in one query for the week 
        List<StudySession> weeklySessions = studySessionRepository
                .findByUserAndStartTimeBetween(user, weekStart, weekEnd);

        if (weeklySessions.isEmpty()) return Collections.emptyList();

        // Collect session ids and fetch all session-subject links in a single query
        List<UUID> sessionIds = weeklySessions.stream()
                .map(StudySession::getId)
                .collect(Collectors.toList());

        List<SessionSubject> allLinks = sessionSubjectRepository.findBySessionIdIn(sessionIds); // get all session-subject links for the week's sessions

        // Group session-subject links by session id for quick lookup
        Map<UUID, List<SessionSubject>> linksBySession = allLinks.stream()
                .collect(Collectors.groupingBy(ss -> ss.getSession().getId()));

        // Aggregate duration per subject
        Map<UUID, Long> subjectDurationMap = new HashMap<>();

        // loop through every session 
        for (StudySession session : weeklySessions) {
            long duration = Optional.ofNullable(session.getDurationSeconds()).orElse(0); // get duration
            if (duration <= 0) continue; // skip empty sessions 

            List<SessionSubject> subjects = linksBySession.getOrDefault(session.getId(), Collections.emptyList()); // get subjects for this session
            if (subjects.isEmpty()) continue; // skip sessions without subjects 

            long perSubject = duration / subjects.size();
            for (SessionSubject ss : subjects) {
                UUID subjectId = ss.getSubject().getId();
                subjectDurationMap.merge(subjectId, perSubject, Long::sum); // accumulate duration for each subject 
                        // Nếu subjectId chưa tồn tại trong map
                        // hêm mới: subjectId -> perSubject
                        // Nếu subjectId đã tồn tại
                        // lấy giá trị cũ (oldValue) và cộng với perSubject. Long::sum == Long.sum(a, b) == a + b 
            }
        }

        if (subjectDurationMap.isEmpty()) return Collections.emptyList(); // no subjects found, return empty list 

        // Fetch subject names in bulk
        List<UUID> subjectIds = new ArrayList<>(subjectDurationMap.keySet()); // get all subject ids that we need to fetch names for 
        Map<UUID, String> subjectNames = subjectRepository.findAllById(subjectIds).stream()
                .collect(Collectors.toMap(
                        s -> s.getId(),
                        s -> s.getName()
                ));  // Build a map of subjectId -> subjectName for quick lookup when building response 

        // Build response list sorted by descending duration
        return subjectDurationMap.entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed()) // sort by duration descending 
                .map(entry -> new WeeklySubjectStatsResponse(
                        entry.getKey(),
                        subjectNames.get(entry.getKey()),
                        entry.getValue()
                ))
                .collect(Collectors.toList());
    }

    public List<WeeklySubjectStatsResponse> getMonthlySubjectStats(UserEntity user) {
        // First day and last day of current month
        LocalDate today = LocalDate.now();
        LocalDate firstDay = today.withDayOfMonth(1);
        LocalDate lastDay = today.withDayOfMonth(today.lengthOfMonth());

        LocalDateTime monthStart = firstDay.atStartOfDay();
        LocalDateTime monthEnd = lastDay.atTime(LocalTime.MAX);

        List<StudySession> monthlySessions = studySessionRepository
                .findByUserAndStartTimeBetween(user, monthStart, monthEnd);

        if (monthlySessions.isEmpty()) return Collections.emptyList();

        List<UUID> sessionIds = monthlySessions.stream()
                .map(StudySession::getId)
                .collect(Collectors.toList());

        List<SessionSubject> allLinks = sessionSubjectRepository.findBySessionIdIn(sessionIds);

        Map<UUID, List<SessionSubject>> linksBySession = allLinks.stream()
                .collect(Collectors.groupingBy(ss -> ss.getSession().getId()));

        Map<UUID, Long> subjectDurationMap = new HashMap<>();

        for (StudySession session : monthlySessions) {
            long duration = Optional.ofNullable(session.getDurationSeconds()).orElse(0);
            if (duration <= 0) continue;

            List<SessionSubject> subjects = linksBySession.getOrDefault(session.getId(), Collections.emptyList());
            if (subjects.isEmpty()) continue;

            long perSubject = duration / subjects.size();
            for (SessionSubject ss : subjects) {
                UUID subjectId = ss.getSubject().getId();
                subjectDurationMap.merge(subjectId, perSubject, Long::sum);
            }
        }

        if (subjectDurationMap.isEmpty()) return Collections.emptyList();

        List<UUID> subjectIds = new ArrayList<>(subjectDurationMap.keySet());
        Map<UUID, String> subjectNames = subjectRepository.findAllById(subjectIds).stream()
                .collect(Collectors.toMap(
                        s -> s.getId(),
                        s -> s.getName()
                ));

        return subjectDurationMap.entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .map(entry -> new WeeklySubjectStatsResponse(
                        entry.getKey(),
                        subjectNames.get(entry.getKey()),
                        entry.getValue()
                ))
                .collect(Collectors.toList());
    }
}
