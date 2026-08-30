package com.bit.recall.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Login request DTO for user authentication.
 * Contains email and password for user login.
 */
@Serdeable
public record LoginRequest(
        @Email(message = "Email should be valid")
        @NotBlank(message = "Email is required")
        @JsonProperty("email") String email,

        @NotBlank(message = "Password is required")
        @JsonProperty("password") String password
) {}

