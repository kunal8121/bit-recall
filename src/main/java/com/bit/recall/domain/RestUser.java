package com.bit.recall.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record RestUser(
        @JsonProperty("username") String name,
        @JsonProperty("email") String email
) {}
