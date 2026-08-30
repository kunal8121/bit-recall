package com.bit.recall.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Serdeable
public record SignUpRequest(
       @JsonProperty("username") @NotBlank String username,
       @JsonProperty("password") @NotBlank String password,
       @Pattern(
               regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$",
               message = "Email format is incorrect"
       )
       @JsonProperty("email") @NotBlank String email) {}
