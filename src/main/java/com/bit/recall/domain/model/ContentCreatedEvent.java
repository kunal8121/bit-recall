package com.bit.recall.domain.model;

import lombok.Builder;

@Builder
public record ContentCreatedEvent(String id, String contentId) {
}
