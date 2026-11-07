package com.bit.recall.service.impl;

import com.bit.recall.config.ContentProcessorConfig;
import com.bit.recall.domain.Content;
import com.bit.recall.repo.ContentRepository;
import com.bit.recall.service.ContentProcessingService;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.bit.recall.domain.model.ContentCreatedEvent;

import static com.bit.recall.domain.Status.*;

@Singleton
@RequiredArgsConstructor
public class ContentProcessingRetryService {

    private final ContentRepository contentRepository;
    private final ContentProcessorConfig config;
    private final ContentProcessingService contentProcessingService;

    @Scheduled(fixedDelay = "1m")
    public void retryFailedOrUnprocessedItems() {
        int staleSeconds = config.staleAfter() == null ? 600 : config.staleAfter();
        int orphanAfterSeconds = config.orphanAfter() == null ? 120 : config.orphanAfter();

        //Fetch items for retry based on the following criteria:
        var failedContents = contentRepository.findByStatus(PROCESSING_FAILED, config.maxRetries(), null);
        var staleContents = contentRepository.findByStatus(PROCESSING, config.maxRetries(), staleSeconds);
        var orphanedContents = contentRepository.findOrphanedUnprocessedItems(config.maxRetries(), orphanAfterSeconds);

        List<Content> contentsToRetry = new ArrayList<>();

        contentsToRetry.addAll(failedContents);
        contentsToRetry.addAll(staleContents);
        contentsToRetry.addAll(orphanedContents);

        if(!contentsToRetry.isEmpty()) {
            contentsToRetry.forEach(content -> {
                contentProcessingService.processContent(ContentCreatedEvent.builder()
                        .id(UUID.randomUUID().toString())
                        .contentId(content.getId().toString())
                        .build());
            });
        }
    }
}
