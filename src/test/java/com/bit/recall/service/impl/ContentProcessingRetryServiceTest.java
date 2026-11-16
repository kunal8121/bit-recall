package com.bit.recall.service.impl;

import com.bit.recall.domain.model.ContentCreatedEvent;
import com.bit.recall.service.ContentProcessingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentProcessingRetryServiceTest {

    @Mock ContentRetryCandidateService retryCandidateService;
    @Mock ContentProcessingService contentProcessingService;

    @Test
    void dispatchesEachRetryCandidateAfterFetchingCandidates() {
        ContentCreatedEvent failed = event("content-failed");
        ContentCreatedEvent stale = event("content-stale");
        ContentCreatedEvent orphaned = event("content-orphaned");
        when(retryCandidateService.findRetryCandidates()).thenReturn(List.of(failed, stale, orphaned));
        ContentProcessingRetryService service = new ContentProcessingRetryService(
                retryCandidateService, contentProcessingService);

        service.retryFailedOrUnprocessedItems();

        verify(contentProcessingService).processContent(failed);
        verify(contentProcessingService).processContent(stale);
        verify(contentProcessingService).processContent(orphaned);
    }

    @Test
    void doesNothingWhenNoContentNeedsRetry() {
        when(retryCandidateService.findRetryCandidates()).thenReturn(List.of());
        ContentProcessingRetryService service = new ContentProcessingRetryService(
                retryCandidateService, contentProcessingService);

        service.retryFailedOrUnprocessedItems();

        verifyNoInteractions(contentProcessingService);
    }

    private ContentCreatedEvent event(String contentId) {
        return ContentCreatedEvent.builder()
                .id("event-" + contentId)
                .contentId(contentId)
                .topicId("topic-id")
                .userId("user-id")
                .build();
    }
}
