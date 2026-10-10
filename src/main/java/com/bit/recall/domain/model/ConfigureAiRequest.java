package com.bit.recall.domain.model;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;

@Serdeable
public record ConfigureAiRequest(
        @NotBlank String apiKey,
        @NotBlank String provider) {}
