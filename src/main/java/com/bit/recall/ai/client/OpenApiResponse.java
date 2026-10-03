package com.bit.recall.ai.client;

import io.micronaut.core.annotation.Introspected;

import java.util.List;

@Introspected
public record OpenApiResponse(List<Choice> choices) {
    public record Choice(Message message) {}
    public record Message(String content) {}
}
