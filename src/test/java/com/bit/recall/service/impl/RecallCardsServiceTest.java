package com.bit.recall.service.impl;

import com.bit.recall.domain.Content;
import com.bit.recall.domain.RecallItem;
import com.bit.recall.domain.Topic;
import com.bit.recall.domain.model.RecallCardDto;
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
}
