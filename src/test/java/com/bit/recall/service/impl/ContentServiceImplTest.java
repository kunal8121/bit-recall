package com.bit.recall.service.impl;

import com.bit.recall.domain.*;
import com.bit.recall.domain.model.CreateContentRequest;
import com.bit.recall.domain.model.ContentCreatedEvent;
import com.bit.recall.exception.BitRecallException;
import com.bit.recall.repo.*;
import com.bit.recall.utils.TokenCounter;
import io.micronaut.context.event.ApplicationEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentServiceImplTest {
    private static final int TOKEN_LIMIT = 100;

    @Mock TopicRepository topicRepository;
    @Mock ContentRepository contentRepository;
    @Mock RecallItemRepository recallItemRepository;
    @Mock TokenCounter tokenCounter;
    @Mock AiConfigureRepository aiConfigureRepository;
    @Mock ApplicationEventPublisher<ContentCreatedEvent> contentPublisher;

    private ContentServiceImpl createService() {
        return new ContentServiceImpl(TOKEN_LIMIT, topicRepository, contentRepository, recallItemRepository, tokenCounter,
                aiConfigureRepository, contentPublisher);
    }

    @Test
    void createsContentWithDefaultRevisionDepthAndPublishesEvent() {
        UUID topicId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Topic topic = Topic.builder().id(topicId).user(User.builder().id(userId).build()).build();
        when(topicRepository.findById(topicId)).thenReturn(Optional.of(topic));
        when(tokenCounter.countTokens("notes")).thenReturn(4);
        when(contentRepository.save(any(Content.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Content content = createService().createContent(topicId.toString(),
                new CreateContentRequest("notes", null), userId.toString());

        assertEquals(RevisionDepth.COMPREHENSIVE, content.getRevisionDepth());
        assertEquals(Status.UNPROCESSED, content.getStatus());
        assertSame(topic, content.getTopic());
        verify(contentPublisher).publishEvent(any());
    }

    @Test
    void rejectsOversizedContentAndInvalidRevisionDepth() {
        UUID topicId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Topic topic = Topic.builder().id(topicId).user(User.builder().id(userId).build()).build();
        when(topicRepository.findById(topicId)).thenReturn(Optional.of(topic));
        when(tokenCounter.countTokens("huge")).thenReturn(TOKEN_LIMIT + 1);
        when(tokenCounter.countTokens("small")).thenReturn(1);
        ContentServiceImpl service = createService();

        assertThrows(BitRecallException.class, () -> service.createContent(topicId.toString(),
                new CreateContentRequest("huge", null), userId.toString()));
        assertThrows(BitRecallException.class, () -> service.createContent(topicId.toString(),
                new CreateContentRequest("small", "unknown"), userId.toString()));
        verifyNoInteractions(contentRepository);
    }

    @Test
    void deletesRecallItemsBeforeDeletingContent() {
        UUID topicId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        Topic topic = Topic.builder().id(topicId).user(User.builder().id(userId).build()).build();
        Content content = Content.builder().id(contentId).topic(topic).build();
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

        createService().deleteContentById(contentId.toString(), userId.toString());

        var inOrder = inOrder(recallItemRepository, contentRepository);
        inOrder.verify(recallItemRepository).deleteByContentId(contentId);
        inOrder.verify(contentRepository).delete(content);
    }
}
