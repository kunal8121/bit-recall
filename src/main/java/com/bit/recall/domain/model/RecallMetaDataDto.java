package com.bit.recall.domain.model;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;

@Builder
@Serdeable
public record RecallMetaDataDto(String type,
                                String example,
                                String code,
                                String pattern) {}
