package com.bit.recall.service.impl;

import com.bit.recall.config.ContentProcessorConfig;
import com.bit.recall.domain.Content;
import com.bit.recall.domain.model.ContentCreatedEvent;
import com.bit.recall.repo.ContentRepository;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor
public class ContentRetryCandidateService {

    private final ContentRepository contentRepository;
    private final ContentProcessorConfig config;

    @Transactional
    public List<ContentCreatedEvent> findRetryCandidates() {
        int staleSeconds = config.staleAfter() == null ? 600 : config.staleAfter();
        int orphanAfterSeconds = config.orphanAfter() == null ? 120 : config.orphanAfter();

        var failedContents = contentRepository.findRetryableFailedContents(config.maxRetries());
        var staleContents = contentRepository.findStaleProcessingContents(
                Instant.now().minusSeconds(staleSeconds));
        var orphanedContents = contentRepository.findOrphanedUnprocessedContents(
               Instant.now().minusSeconds(orphanAfterSeconds));

        List<Content> contentsToRetry = new ArrayList<>();
        contentsToRetry.addAll(failedContents);
        contentsToRetry.addAll(staleContents);
        contentsToRetry.addAll(orphanedContents);

        return contentsToRetry.stream()
                .map(content -> ContentCreatedEvent.builder()
                        .id(UUID.randomUUID().toString())
                        .contentId(content.getId().toString())
                        .topicId(content.getTopic().getId().toString())
                        .userId(content.getTopic().getUser().getId().toString())
                        .build())
                .toList();
    }
}
