package com.bit.recall.service.impl;

import com.bit.recall.config.ContentProcessorConfig;
import com.bit.recall.domain.Content;
import com.bit.recall.domain.Status;
import com.bit.recall.domain.Topic;
import com.bit.recall.domain.User;
import com.bit.recall.domain.model.ContentCreatedEvent;
import com.bit.recall.repo.ContentRepository;
import com.bit.recall.service.ContentProcessingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentProcessingRetryServiceTest {
    @Mock ContentRepository contentRepository;
    @Mock ContentProcessingService contentProcessingService;

    @Test
    void dispatchesFailedContentForRetry() {
        Content failedContent = contentWithOwner();
        when(contentRepository.findByStatus(Status.PROCESSING_FAILED.name(), 3, null))
                .thenReturn(List.of(failedContent));
        when(contentRepository.findByStatus(Status.PROCESSING.name(), 3, 600)).thenReturn(List.of());
        when(contentRepository.findOrphanedUnprocessedItems(3, 600)).thenReturn(List.of());
        ContentProcessingRetryService service = new ContentProcessingRetryService(contentRepository,
                new ContentProcessorConfig(600, 600, 3), contentProcessingService);

        service.retryFailedOrUnprocessedItems();

        assertDispatchedContentId(failedContent.getId());
    }

    @Test
    void dispatchesStaleProcessingContentForRetry() {
        Content staleContent = contentWithOwner();
        when(contentRepository.findByStatus(Status.PROCESSING_FAILED.name(), 3, null)).thenReturn(List.of());
        when(contentRepository.findByStatus(Status.PROCESSING.name(), 3, 600)).thenReturn(List.of(staleContent));
        when(contentRepository.findOrphanedUnprocessedItems(3, 600)).thenReturn(List.of());
        ContentProcessingRetryService service = new ContentProcessingRetryService(contentRepository,
                new ContentProcessorConfig(600, 600, 3), contentProcessingService);

        service.retryFailedOrUnprocessedItems();

        assertDispatchedContentId(staleContent.getId());
    }

    @Test
    void dispatchesUnprocessedContentForRetry() {
        Content unprocessedContent = contentWithOwner();
        when(contentRepository.findByStatus(Status.PROCESSING_FAILED.name(), 3, null)).thenReturn(List.of());
        when(contentRepository.findByStatus(Status.PROCESSING.name(), 3, 600)).thenReturn(List.of());
        when(contentRepository.findOrphanedUnprocessedItems(3, 600)).thenReturn(List.of(unprocessedContent));
        ContentProcessingRetryService service = new ContentProcessingRetryService(contentRepository,
                new ContentProcessorConfig(600, 600, 3), contentProcessingService);

        service.retryFailedOrUnprocessedItems();

        assertDispatchedContentId(unprocessedContent.getId());
    }

    @Test
    void doesNothingWhenNoContentNeedsRetry() {
        when(contentRepository.findByStatus(any(), any(), any())).thenReturn(List.of());
        when(contentRepository.findOrphanedUnprocessedItems(any(), anyInt())).thenReturn(List.of());
        ContentProcessingRetryService service = new ContentProcessingRetryService(contentRepository,
                new ContentProcessorConfig(null, null, 2), contentProcessingService);

        service.retryFailedOrUnprocessedItems();

        verifyNoInteractions(contentProcessingService);
    }

    private void assertDispatchedContentId(UUID expectedContentId) {
        ArgumentCaptor<ContentCreatedEvent> eventCaptor = ArgumentCaptor.forClass(ContentCreatedEvent.class);
        verify(contentProcessingService).processContent(eventCaptor.capture());

        assertEquals(expectedContentId.toString(), eventCaptor.getValue().contentId());
        assertEquals("00000000-0000-0000-0000-000000000001", eventCaptor.getValue().topicId());
        assertEquals("00000000-0000-0000-0000-000000000002", eventCaptor.getValue().userId());
    }

    private Content contentWithOwner() {
        return Content.builder()
                .id(UUID.randomUUID())
                .topic(Topic.builder()
                        .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                        .user(User.builder().id(UUID.fromString("00000000-0000-0000-0000-000000000002")).build())
                        .build())
                .build();
    }
}
