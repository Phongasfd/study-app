package com.namphong.backend.service;

import com.namphong.backend.dto.JoinRequestResponse;
import com.namphong.backend.dto.JoinResultResponse;
import com.namphong.backend.entity.*;
import com.namphong.backend.exception.BadRequestException;
import com.namphong.backend.exception.NotFoundException;
import com.namphong.backend.repository.GroupJoinRequestRepository;
import com.namphong.backend.repository.GroupMemberRepository;
import com.namphong.backend.repository.GroupRepository;
import com.namphong.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Joining groups: a PUBLIC group is joined at once, a PRIVATE group through a request
 * that the owner approves or rejects.
 */
@Service
@RequiredArgsConstructor
public class GroupJoinService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupJoinRequestRepository joinRequestRepository;
    private final UserRepository userRepository;
    private final GroupMemberService groupMemberService;
    private final NotificationService notificationService;
    private final Clock clock;

    @Transactional
    public JoinResultResponse join(UUID groupId, UUID userId, String message) {
        StudyGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (groupMemberService.isMember(groupId, userId)) {
            throw new BadRequestException("User is already a member of this group");
        }

        if (group.getVisibility() != GroupVisibility.PRIVATE) {
            groupMemberService.addMember(group, user);
            return new JoinResultResponse(JoinResultResponse.JOINED, groupId, null);
        }

        // asking twice returns the open request instead of failing
        var pending = joinRequestRepository.findByGroupIdAndUserIdAndStatus(groupId, userId, JoinRequestStatus.PENDING);
        if (pending.isPresent()) {
            return new JoinResultResponse(JoinResultResponse.REQUESTED, groupId, pending.get().getId());
        }
        if (group.getMaxMembers() != null && groupMemberRepository.countByGroupId(groupId) >= group.getMaxMembers()) {
            throw new BadRequestException("Group is full");
        }

        GroupJoinRequest request = new GroupJoinRequest();
        request.setGroup(group);
        request.setUser(user);
        request.setMessage(message == null || message.isBlank() ? null : message.trim());
        request.setStatus(JoinRequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now(clock));
        // two simultaneous requests: the partial unique index lets one in, the other gets 409
        GroupJoinRequest saved = joinRequestRepository.saveAndFlush(request);

        notificationService.notify(group.getOwner().getId(), NotificationType.JOIN_REQUEST_RECEIVED,
                user.getUsername() + " wants to join " + group.getName(), group.getName(), "/groups/" + groupId);
        return new JoinResultResponse(JoinResultResponse.REQUESTED, groupId, saved.getId());
    }

    @Transactional(readOnly = true)
    public List<JoinRequestResponse> pendingForGroup(UUID groupId, UUID requesterId) {
        requireOwner(groupId, requesterId);
        return joinRequestRepository.findWithUserByGroup(groupId, JoinRequestStatus.PENDING).stream()
                .map(r -> toResponse(r, groupId, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<JoinRequestResponse> pendingForUser(UUID userId) {
        return joinRequestRepository.findWithGroupByUser(userId, JoinRequestStatus.PENDING).stream()
                .map(r -> new JoinRequestResponse(r.getId(), r.getGroup().getId(), r.getGroup().getName(), userId,
                        null, r.getMessage(), r.getStatus(), r.getCreatedAt()))
                .toList();
    }

    /**
     * Approves a pending request and adds the member, all or nothing: if the group filled up
     * meanwhile, the whole transaction rolls back and the request stays PENDING.
     */
    @Transactional
    public void approve(UUID groupId, UUID requestId, UUID ownerId) {
        StudyGroup group = requireOwner(groupId, ownerId);
        // the conditional UPDATE picks one winner among concurrent approve/reject/cancel calls
        if (joinRequestRepository.decide(requestId, groupId, JoinRequestStatus.APPROVED,
                LocalDateTime.now(clock), ownerId) == 0) {
            throw alreadyHandledOrMissing(requestId, groupId);
        }
        GroupJoinRequest request = joinRequestRepository.findWithUserAndGroup(requestId).orElseThrow();
        UserEntity user = request.getUser();

        // added by the owner directly in the meantime: nothing more to do
        if (!groupMemberService.isMember(groupId, user.getId())) {
            groupMemberService.addMember(group, user);
        }
        notificationService.notify(user.getId(), NotificationType.JOIN_REQUEST_APPROVED,
                "Your request to join " + group.getName() + " was approved", group.getName(), "/groups/" + groupId);
    }

    @Transactional
    public void reject(UUID groupId, UUID requestId, UUID ownerId) {
        StudyGroup group = requireOwner(groupId, ownerId);
        if (joinRequestRepository.decide(requestId, groupId, JoinRequestStatus.REJECTED,
                LocalDateTime.now(clock), ownerId) == 0) {
            throw alreadyHandledOrMissing(requestId, groupId);
        }
        GroupJoinRequest request = joinRequestRepository.findWithUserAndGroup(requestId).orElseThrow();
        notificationService.notify(request.getUser().getId(), NotificationType.JOIN_REQUEST_REJECTED,
                "Your request to join " + group.getName() + " was declined", group.getName(), "/groups");
    }

    /** The requester withdraws their own pending request. */
    @Transactional
    public void cancel(UUID groupId, UUID userId) {
        GroupJoinRequest request = joinRequestRepository
                .findByGroupIdAndUserIdAndStatus(groupId, userId, JoinRequestStatus.PENDING)
                .orElseThrow(() -> new NotFoundException("No pending request for this group"));
        if (joinRequestRepository.decide(request.getId(), groupId, JoinRequestStatus.CANCELLED,
                LocalDateTime.now(clock), userId) == 0) {
            throw new BadRequestException("This request was already handled");
        }
    }

    private StudyGroup requireOwner(UUID groupId, UUID userId) {
        StudyGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));
        if (!group.isOwnedBy(userId)) {
            throw new AccessDeniedException("Only the owner can manage join requests");
        }
        return group;
    }

    private RuntimeException alreadyHandledOrMissing(UUID requestId, UUID groupId) {
        boolean exists = joinRequestRepository.findById(requestId)
                .filter(r -> r.getGroup().getId().equals(groupId))
                .isPresent();
        return exists ? new BadRequestException("This request was already handled")
                      : new NotFoundException("Join request not found");
    }

    private static JoinRequestResponse toResponse(GroupJoinRequest r, UUID groupId, String groupName) {
        return new JoinRequestResponse(r.getId(), groupId, groupName, r.getUser().getId(), r.getUser().getUsername(),
                r.getMessage(), r.getStatus(), r.getCreatedAt());
    }
}
