package com.namphong.backend.controller;

import com.namphong.backend.dto.ChatMessageRequest;
import com.namphong.backend.dto.ChatMessageResponse;
import com.namphong.backend.entity.UserEntity;
import com.namphong.backend.exception.BadRequestException;
import com.namphong.backend.service.ChatMessageService;
import com.namphong.backend.service.GroupMemberService;
import com.namphong.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/chat")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final UserService userService;
    private final GroupMemberService groupMemberService;

    @PostMapping
    public ResponseEntity<ChatMessageResponse> createMessage(@RequestBody ChatMessageRequest request,
                                                           HttpServletRequest httpRequest) {
        if (httpRequest.getUserPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }
        UUID userId = UUID.fromString(httpRequest.getUserPrincipal().getName());
        groupMemberService.requireMember(parseGroupId(request.getGroupId()), userId);
        UserEntity currentUser = userService.getUserEntityById(userId);

        ChatMessageResponse response = chatMessageService.createMessage(request, currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<List<ChatMessageResponse>> getMessagesByGroupId(@PathVariable String groupId,
                                                                          HttpServletRequest httpRequest) {
        // only members can read a group chat history
        UUID userId = UUID.fromString(httpRequest.getUserPrincipal().getName());
        groupMemberService.requireMember(parseGroupId(groupId), userId);
        List<ChatMessageResponse> messages = chatMessageService.getMessagesByGroupId(groupId);
        return ResponseEntity.ok(messages);
    }

    private UUID parseGroupId(String groupId) {
        try {
            return UUID.fromString(groupId);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadRequestException("Invalid groupId");
        }
    }
}
