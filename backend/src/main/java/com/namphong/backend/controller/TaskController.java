package com.namphong.backend.controller;

import com.namphong.backend.dto.TaskRequest;
import com.namphong.backend.dto.TaskResponse;
import com.namphong.backend.dto.TaskStatusRequest;
import com.namphong.backend.dto.TaskSummaryResponse;
import com.namphong.backend.entity.TaskStatus;
import com.namphong.backend.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService taskService;

    /** Filters are optional: status, subjectId, due = overdue | today | week. */
    @GetMapping
    public List<TaskResponse> list(@RequestParam(required = false) TaskStatus status,
                                   @RequestParam(required = false) UUID subjectId,
                                   @RequestParam(required = false) String due,
                                   Principal principal) {
        return taskService.list(CurrentUser.id(principal), status, subjectId, due);
    }

    @GetMapping("/summary")
    public TaskSummaryResponse summary(Principal principal) {
        return taskService.summary(CurrentUser.id(principal));
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request, Principal principal) {
        return ResponseEntity.status(201).body(taskService.create(CurrentUser.id(principal), request));
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable UUID id, @Valid @RequestBody TaskRequest request, Principal principal) {
        return taskService.update(CurrentUser.id(principal), id, request);
    }

    @PatchMapping("/{id}/status")
    public TaskResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody TaskStatusRequest request,
                                     Principal principal) {
        return taskService.changeStatus(CurrentUser.id(principal), id, request.status());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Principal principal) {
        taskService.delete(CurrentUser.id(principal), id);
        return ResponseEntity.noContent().build();
    }
}
