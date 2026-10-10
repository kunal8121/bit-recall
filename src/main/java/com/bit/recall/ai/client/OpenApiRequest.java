package com.bit.recall.ai.client;

import lombok.Builder;

import java.util.List;

@Builder
public record OpenApiRequest(
        String model,
        ResponseFormat responseFormat,
        List<Message> messages) {
    @Builder
    public record ResponseFormat(String type) {}
    @Builder
    public record Message(String role, String content) {}
}
