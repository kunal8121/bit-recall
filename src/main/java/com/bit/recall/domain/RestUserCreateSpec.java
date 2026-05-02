package com.bit.recall.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record RestUserCreateSpec(
       @JsonProperty("username") String name,
       @JsonProperty("password") String password,
       @JsonProperty("email") String email) {}
