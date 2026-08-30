package com.bit.recall.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record RestContent(
       @JsonProperty("content_id") String UUID,
       @JsonProperty("topicId") TopicResponse topic,
       @JsonProperty("content_text") String text,
       @JsonProperty("created_at") Instant createdAt) {}
