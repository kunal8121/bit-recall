package com.bit.recall.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record UserResponse(
        @JsonProperty("id") String id,
        @JsonProperty("username") String username,
        @JsonProperty("email") String email
) {}
