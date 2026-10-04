package com.bit.recall.ai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.Introspected;

import java.util.List;

@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenApiResponse(List<Choice> choices, Error error) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(Message message, @JsonProperty("finish_reason") String finishReason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(String content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Error(String message, String type, String code) {}
}
