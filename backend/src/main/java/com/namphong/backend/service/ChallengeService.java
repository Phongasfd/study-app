package com.namphong.backend.service;

import com.namphong.backend.dto.ChallengeDetailResponse;
import com.namphong.backend.dto.ChallengeParticipantResponse;
import com.namphong.backend.dto.ChallengeRequest;
import com.namphong.backend.dto.ChallengeResponse;
import com.namphong.backend.entity.*;
import com.namphong.backend.exception.BadRequestException;
import com.namphong.backend.exception.NotFoundException;
import com.namphong.backend.repository.GroupChallengeRepository;
import com.namphong.backend.repository.GroupMemberRepository;
import com.namphong.backend.repository.GroupRepository;
import com.namphong.backend.repository.StudySessionRepository;
import com.namphong.backend.repository.StudySessionRepository.SessionSlice;
import com.namphong.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Group challenges ("study 20 hours this week"). Progress is computed from completed sessions
 * on read; a challenge board costs 3 queries whatever the group size.
 */
@Service
@RequiredArgsConstructor
public class ChallengeService {

    static final int MAX_DAYS = 90;
    static final int MAX_BACKDATE_DAYS = 30;

    private final GroupChallengeRepository challengeRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final StudySessionRepository studySessionRepository;
    private final UserRepository userRepository;
    private final GroupMemberService groupMemberService;
    private final NotificationService notificationService;
    private final Clock clock;

    /** All challenges of the group with the requester's own progress (2 queries). */
    @Transactional(readOnly = true)
    public List<ChallengeResponse> list(UUID groupId, UUID userId) {
        groupMemberService.requireMember(groupId, userId);
        List<GroupChallenge> challenges = challengeRepository.findByGroupIdWithCreator(groupId);
        if (challenges.isEmpty()) {
            return List.of();
        }
        LocalDate today = LocalDate.now(clock);
        LocalDate from = challenges.stream().map(GroupChallenge::getStartDate).min(Comparator.naturalOrder()).orElseThrow();
        LocalDate to = challenges.stream().map(GroupChallenge::getEndDate).max(Comparator.naturalOrder()).orElseThrow();
        // one query for the whole range, bucketed per challenge below
        List<SessionSlice> mine = studySessionRepository.findCompletedSlices(
                userId, from.atStartOfDay(), to.plusDays(1).atStartOfDay());

        return challenges.stream()
                .map(c -> toResponse(c, today, secondsWithin(mine, c.getStartDate(), c.getEndDate())))
                .toList();
    }

    /** One challenge with every member's progress, ranked (ties share a rank). */
    @Transactional(readOnly = true)
    public ChallengeDetailResponse detail(UUID groupId, UUID challengeId, UUID userId) {
        groupMemberService.requireMember(groupId, userId);
        GroupChallenge challenge = challengeRepository.findInGroup(challengeId, groupId)
                .orElseThrow(() -> new NotFoundException("Challenge not found"));

        List<GroupMember> members = groupMemberRepository.findAllWithUserByGroupId(groupId);
        List<UUID> userIds = members.stream().map(m -> m.getUser().getId()).toList();
        Map<UUID, Long> seconds = userIds.isEmpty() ? Map.of() : studySessionRepository
                .sumDurationByUserIds(userIds, SessionStatus.COMPLETED,
                        challenge.getStartDate().atStartOfDay(), challenge.getEndDate().plusDays(1).atStartOfDay())
                .stream()
                .collect(Collectors.toMap(StudySessionRepository.UserDuration::getUserId,
                        row -> row.getTotalSeconds() != null ? row.getTotalSeconds() : 0L));

        int target = challenge.getTargetSeconds();
        List<GroupMember> ranked = members.stream()
                .sorted(Comparator.comparingLong((GroupMember m) -> seconds.getOrDefault(m.getUser().getId(), 0L))
                        .reversed()
                        .thenComparing(m -> m.getUser().getUsername()))
                .toList();

        List<ChallengeParticipantResponse> participants = new ArrayList<>(ranked.size());
        long previous = -1;
        int rank = 0;
        for (int i = 0; i < ranked.size(); i++) {
            UserEntity user = ranked.get(i).getUser();
            long progress = seconds.getOrDefault(user.getId(), 0L);
            if (progress != previous) {
                rank = i + 1;
                previous = progress;
            }
            participants.add(new ChallengeParticipantResponse(user.getId(), user.getUsername(), progress,
                    percent(progress, target), progress >= target, rank));
        }
        int completed = (int) participants.stream().filter(ChallengeParticipantResponse::completed).count();

        LocalDate today = LocalDate.now(clock);
        ChallengeResponse summary = toResponse(challenge, today, seconds.getOrDefault(userId, 0L));
        return new ChallengeDetailResponse(summary, participants, completed);
    }

    @Transactional
    public ChallengeResponse create(UUID groupId, UUID userId, ChallengeRequest request) {
        StudyGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));
        if (!group.isOwnedBy(userId)) {
            throw new AccessDeniedException("Only the owner can create challenges");
        }
        LocalDate today = LocalDate.now(clock);
        validateDates(request.startDate(), request.endDate(), today);

        GroupChallenge challenge = new GroupChallenge();
        challenge.setGroup(group);
        challenge.setCreatedBy(userRepository.getReferenceById(userId));
        challenge.setTitle(request.title().trim());
        challenge.setDescription(request.description() == null || request.description().isBlank()
                ? null : request.description().trim());
        challenge.setTargetSeconds((int) Math.round(request.targetHours() * 3600));
        challenge.setStartDate(request.startDate());
        challenge.setEndDate(request.endDate());
        challenge.setCreatedAt(LocalDateTime.now(clock));
        GroupChallenge saved = challengeRepository.save(challenge);

        List<UUID> others = groupMemberRepository.findAllWithUserByGroupId(groupId).stream()
                .map(m -> m.getUser().getId())
                .filter(id -> !id.equals(userId))
                .toList();
        notificationService.notifyAll(others, NotificationType.CHALLENGE_CREATED,
                "New challenge in " + group.getName() + ": " + saved.getTitle(), saved.getTitle(),
                "/groups/" + groupId);

        // creator name: the owner, already loaded with the group
        return new ChallengeResponse(saved.getId(), groupId, saved.getTitle(), saved.getDescription(),
                saved.getTargetSeconds(), saved.getStartDate(), saved.getEndDate(), saved.statusOn(today),
                group.getOwner().getUsername(), 0, 0);
    }

    @Transactional
    public void delete(UUID groupId, UUID challengeId, UUID userId) {
        GroupChallenge challenge = challengeRepository.findInGroup(challengeId, groupId)
                .orElseThrow(() -> new NotFoundException("Challenge not found"));
        if (!challenge.getGroup().isOwnedBy(userId)) {
            throw new AccessDeniedException("Only the owner can delete challenges");
        }
        challengeRepository.delete(challenge);
    }

    // ---------------------------------------------------------------------------------

    static void validateDates(LocalDate start, LocalDate end, LocalDate today) {
        if (end.isBefore(start)) {
            throw new BadRequestException("The end date must not be before the start date");
        }
        if (end.isBefore(today)) {
            throw new BadRequestException("The challenge would already be over");
        }
        if (start.isBefore(today.minusDays(MAX_BACKDATE_DAYS))) {
            throw new BadRequestException("The start date can be at most " + MAX_BACKDATE_DAYS + " days ago");
        }
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_DAYS) {
            throw new BadRequestException("A challenge can last at most " + MAX_DAYS + " days");
        }
    }

    /** Seconds of sessions started on days [first, last]. */
    static long secondsWithin(List<SessionSlice> sessions, LocalDate first, LocalDate last) {
        LocalDateTime from = first.atStartOfDay();
        LocalDateTime to = last.plusDays(1).atStartOfDay();
        long total = 0;
        for (SessionSlice s : sessions) {
            if (s.getStartTime() != null && s.getDurationSeconds() != null
                    && !s.getStartTime().isBefore(from) && s.getStartTime().isBefore(to)) {
                total += s.getDurationSeconds();
            }
        }
        return total;
    }

    static int percent(long progress, int target) {
        return (int) Math.min(100, progress * 100 / Math.max(1, target));
    }

    private static ChallengeResponse toResponse(GroupChallenge c, LocalDate today, long myProgress) {
        return new ChallengeResponse(c.getId(), c.getGroup().getId(), c.getTitle(), c.getDescription(),
                c.getTargetSeconds(), c.getStartDate(), c.getEndDate(), c.statusOn(today),
                c.getCreatedBy().getUsername(), myProgress, percent(myProgress, c.getTargetSeconds()));
    }
}
