package com.bit.recall.domain.model;

import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;

@Serdeable
public record RecallCardResponse(String id,
                                 String contentId,
                                 String topicId,
                                 String title,
                                 String question,
                                 String summaryBody,
                                 MetaData metaData,
                                 Instant createdAt) {

    @Serdeable
    public record MetaData(
            String type,
            String example,
            String code,
            String pattern){}
}
