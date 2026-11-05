package com.namphong.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

/** Single-instance delivery: every member socket is on this JVM, so write to them directly. */
public class LocalGroupEventBus implements GroupEventBus {

    private final GroupSessionRegistry registry;
    private final ObjectMapper objectMapper;

    public LocalGroupEventBus(GroupSessionRegistry registry, ObjectMapper objectMapper) {
        this.registry = registry;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(UUID groupId, String type, Object data) {
        registry.deliverLocally(groupId, GroupEventBus.toJson(objectMapper, type, data));
    }
}
