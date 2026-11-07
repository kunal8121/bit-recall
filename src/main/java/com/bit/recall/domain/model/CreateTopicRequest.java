package com.bit.recall.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

@Serdeable
public record CreateTopicRequest(@NotBlank String title,
                                 @JsonProperty("description") String description ) {}