package com.bit.recall.service.impl;

import com.bit.recall.config.ContentProcessorConfig;
import com.bit.recall.domain.Content;
import com.bit.recall.domain.Status;
import com.bit.recall.repo.ContentRepository;
import com.bit.recall.service.ContentProcessingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentProcessingRetryServiceTest {
    @Mock ContentRepository contentRepository;
    @Mock ContentProcessingService contentProcessingService;

    @Test
    void dispatchesFailedStaleAndUnprocessedContent() {
        Content failedContent = Content.builder().id(UUID.randomUUID()).build();
        when(contentRepository.findByStatus(Status.PROCESSING_FAILED, 3, null))
                .thenReturn(List.of(failedContent));
        when(contentRepository.findByStatus(Status.PROCESSING, 3, 600)).thenReturn(List.of());
        when(contentRepository.findByStatus(Status.UNPROCESSED, 3, null)).thenReturn(List.of());
        ContentProcessingRetryService service = new ContentProcessingRetryService(contentRepository,
                new ContentProcessorConfig(600, 600, 3), contentProcessingService);

        service.retryFailedOrUnprocessedItems();

        verify(contentProcessingService).processContent(argThat(event ->
                event.contentId().equals(failedContent.getId().toString())));
    }

    @Test
    void doesNothingWhenNoContentNeedsRetry() {
        when(contentRepository.findByStatus(any(), any(), any())).thenReturn(List.of());
        ContentProcessingRetryService service = new ContentProcessingRetryService(contentRepository,
                new ContentProcessorConfig(null, null, 2), contentProcessingService);

        service.retryFailedOrUnprocessedItems();

        verifyNoInteractions(contentProcessingService);
    }
}
