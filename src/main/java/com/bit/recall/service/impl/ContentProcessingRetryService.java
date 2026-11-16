package com.bit.recall.service.impl;

import com.bit.recall.service.ContentProcessingService;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor
public class ContentProcessingRetryService {

    private final ContentRetryCandidateService retryCandidateService;
    private final ContentProcessingService contentProcessingService;

    @Scheduled(fixedDelay = "1m")
    public void retryFailedOrUnprocessedItems() {
        // Materialize immutable event data inside the query transaction, then dispatch
        // only after it has closed so async processing cannot share its Hibernate session.
        retryCandidateService.findRetryCandidates()
                .forEach(contentProcessingService::processContent);
    }
}
