package com.bit.recall.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;

@Serdeable
public record CreateContentRequest(
        @JsonProperty("content_text") @NotBlank String text,
        @JsonProperty("revision_depth") String revisionDepth) {

}
