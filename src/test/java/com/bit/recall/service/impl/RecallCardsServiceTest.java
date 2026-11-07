package com.bit.recall.service.impl;

import com.bit.recall.domain.Content;
import com.bit.recall.domain.RecallItem;
import com.bit.recall.domain.Topic;
import com.bit.recall.domain.model.RecallCardDto;
import com.bit.recall.domain.model.CursorData;
import com.bit.recall.domain.model.CursorResponse;
import com.bit.recall.domain.model.RecallCardResponse;
import com.bit.recall.exception.BitRecallException;
import com.bit.recall.mapper.RestRecallCardMapper;
import com.bit.recall.repo.ContentRepository;
import com.bit.recall.repo.RecallItemRepository;
import com.bit.recall.repo.TopicRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecallCardsServiceTest {

    @Mock RecallItemRepository recallItemRepository;
    @Mock ContentRepository contentRepository;
    @Mock TopicRepository topicRepository;
    @Mock RestRecallCardMapper recallCardMapper;

    private RecallCardsService createService() {
        return new RecallCardsService(recallItemRepository, contentRepository,
                topicRepository, recallCardMapper);
    }

    @Test
    void savesGeneratedCardsAndMarksAttemptProcessed() throws Exception {
        UUID contentId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        Content content = Content.builder().id(contentId).build();
        Topic topic = Topic.builder().id(UUID.randomUUID()).build();
        when(recallItemRepository.findByContentId(contentId)).thenReturn(List.of());
        when(contentRepository.markAttemptProcessed(contentId, attemptId)).thenReturn(1);
        RecallCardDto card = RecallCardDto.builder().title("Title").recall("Question")
                .explanation("Answer").type("concept").build();

        createService().persist(List.of(card), topic, content, attemptId);

        ArgumentCaptor<RecallItem> savedItem = ArgumentCaptor.forClass(RecallItem.class);
        verify(recallItemRepository).save(savedItem.capture());
        assertEquals("Question", savedItem.getValue().getQuestion());
        assertEquals("Title", savedItem.getValue().getTitle());
        assertTrue(savedItem.getValue().getMetaData().contains("concept"));
        verify(contentRepository).markAttemptProcessed(contentId, attemptId);
    }

    @Test
    void failsIfProcessingAttemptNoLongerOwnsContent() {
        UUID contentId = UUID.randomUUID();
        Content content = Content.builder().id(contentId).build();
        when(recallItemRepository.findByContentId(contentId)).thenReturn(List.of());
        when(contentRepository.markAttemptProcessed(eq(contentId), any())).thenReturn(0);

        assertThrows(IllegalStateException.class, () -> createService().persist(List.of(),
                Topic.builder().build(), content, UUID.randomUUID()));
    }

    @Test
    void requiresTopicOwnershipBeforeFetchingCards() {
        UUID topicId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(topicRepository.findByIdAndUserId(topicId, userId)).thenReturn(Optional.empty());

        assertThrows(BitRecallException.class,
                () -> createService().fetchRecallCardsByTopicId(topicId, userId, 10, null));
        verifyNoInteractions(recallItemRepository, recallCardMapper);
    }

    @Test
    void firstPageReturnsRequestedCardsAndCursorWhenMoreResultsExist() {
        UUID topicId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        int pageSize = 2;
        Instant firstCreatedAt = Instant.parse("2026-01-03T10:00:00Z");
        Instant lastCreatedAt = Instant.parse("2026-01-02T10:00:00Z");
        RecallItem first = recallItem(UUID.randomUUID(), firstCreatedAt);
        RecallItem lastOnPage = recallItem(UUID.randomUUID(), lastCreatedAt);
        RecallItem extra = recallItem(UUID.randomUUID(), Instant.parse("2026-01-01T10:00:00Z"));
        RecallCardResponse firstResponse = responseFor(first);
        RecallCardResponse lastResponse = responseFor(lastOnPage);
        when(topicRepository.findByIdAndUserId(topicId, userId)).thenReturn(Optional.of(Topic.builder().build()));
        when(recallItemRepository.findByTopicId(topicId, userId, pageSize + 1, null, null))
                .thenReturn(List.of(first, lastOnPage, extra));
        when(recallCardMapper.toRecallCardResponse(first)).thenReturn(firstResponse);
        when(recallCardMapper.toRecallCardResponse(lastOnPage)).thenReturn(lastResponse);

        CursorResponse<RecallCardResponse> result = createService()
                .fetchRecallCardsByTopicId(topicId, userId, pageSize, null);

        assertEquals(pageSize, result.pageSize());
        assertTrue(result.hasNext());
        assertEquals(List.of(firstResponse, lastResponse), result.data());
        CursorData nextCursor = CursorData.decodeCursor(result.nextCursor());
        assertEquals(lastCreatedAt, nextCursor.lastCreatedAt());
        assertEquals(lastOnPage.getId(), nextCursor.entityId());
        verify(recallCardMapper, never()).toRecallCardResponse(extra);
    }

    @Test
    void nextPageUsesDecodedCursorAndReturnsNoNextCursorWhenAtEnd() {
        UUID topicId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        int pageSize = 2;
        CursorData previousPage = new CursorData(Instant.parse("2026-02-02T12:30:00Z"), UUID.randomUUID());
        String cursor = CursorData.encodeCursor(previousPage);
        RecallItem nextItem = recallItem(UUID.randomUUID(), Instant.parse("2026-02-02T12:00:00Z"));
        RecallCardResponse nextResponse = responseFor(nextItem);
        when(topicRepository.findByIdAndUserId(topicId, userId)).thenReturn(Optional.of(Topic.builder().build()));
        when(recallItemRepository.findByTopicId(topicId, userId, pageSize + 1,
                previousPage.lastCreatedAt(), previousPage.entityId())).thenReturn(List.of(nextItem));
        when(recallCardMapper.toRecallCardResponse(nextItem)).thenReturn(nextResponse);

        CursorResponse<RecallCardResponse> result = createService()
                .fetchRecallCardsByTopicId(topicId, userId, pageSize, cursor);

        assertEquals(pageSize, result.pageSize());
        assertFalse(result.hasNext());
        assertNull(result.nextCursor());
        assertEquals(List.of(nextResponse), result.data());
        verify(recallItemRepository).findByTopicId(topicId, userId, pageSize + 1,
                previousPage.lastCreatedAt(), previousPage.entityId());
    }

    @Test
    void returnsEmptyFinalPageWhenRepositoryHasNoCards() {
        UUID topicId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(topicRepository.findByIdAndUserId(topicId, userId)).thenReturn(Optional.of(Topic.builder().build()));
        when(recallItemRepository.findByTopicId(topicId, userId, 6, null, null)).thenReturn(List.of());

        CursorResponse<RecallCardResponse> result = createService()
                .fetchRecallCardsByTopicId(topicId, userId, 5, "");

        assertEquals(5, result.pageSize());
        assertFalse(result.hasNext());
        assertNull(result.nextCursor());
        assertTrue(result.data().isEmpty());
        verifyNoInteractions(recallCardMapper);
    }

    @Test
    void exactPageSizeDoesNotCreateNextCursor() {
        UUID topicId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        int pageSize = 2;
        RecallItem first = recallItem(UUID.randomUUID(), Instant.parse("2026-03-02T10:00:00Z"));
        RecallItem second = recallItem(UUID.randomUUID(), Instant.parse("2026-03-01T10:00:00Z"));
        when(topicRepository.findByIdAndUserId(topicId, userId)).thenReturn(Optional.of(Topic.builder().build()));
        when(recallItemRepository.findByTopicId(topicId, userId, pageSize + 1, null, null))
                .thenReturn(List.of(first, second));
        when(recallCardMapper.toRecallCardResponse(first)).thenReturn(responseFor(first));
        when(recallCardMapper.toRecallCardResponse(second)).thenReturn(responseFor(second));

        CursorResponse<RecallCardResponse> result = createService()
                .fetchRecallCardsByTopicId(topicId, userId, pageSize, null);

        assertEquals(pageSize, result.data().size());
        assertFalse(result.hasNext());
        assertNull(result.nextCursor());
    }

    @Test
    void rejectsMalformedCursorBeforeQueryingRecallItems() {
        UUID topicId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(topicRepository.findByIdAndUserId(topicId, userId)).thenReturn(Optional.of(Topic.builder().build()));

        assertThrows(IllegalArgumentException.class, () -> createService()
                .fetchRecallCardsByTopicId(topicId, userId, 5, "not-a-valid-cursor"));

        verifyNoInteractions(recallItemRepository, recallCardMapper);
    }

    private RecallItem recallItem(UUID id, Instant createdAt) {
        return RecallItem.builder().id(id).createdAt(createdAt).build();
    }

    private RecallCardResponse responseFor(RecallItem item) {
        return new RecallCardResponse(item.getId().toString(), "content-id", "topic-id",
                "title", "question", "summary", null, item.getCreatedAt());
    }
}
