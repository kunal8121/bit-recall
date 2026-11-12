package com.bit.recall.service.impl;

import com.bit.recall.domain.Content;
import com.bit.recall.domain.RevisionDepth;
import com.bit.recall.domain.Status;
import com.bit.recall.domain.Topic;
import com.bit.recall.domain.User;
import com.bit.recall.domain.model.CreateContentRequest;
import com.bit.recall.domain.model.ContentCreatedEvent;
import com.bit.recall.exception.BitRecallException;
import com.bit.recall.repo.AiConfigureRepository;
import com.bit.recall.repo.ContentRepository;
import com.bit.recall.repo.RecallItemRepository;
import com.bit.recall.repo.TopicRepository;
import com.bit.recall.utils.TokenCounter;
import io.micronaut.context.event.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentServiceOwnershipTest {
    @Mock TopicRepository topicRepository;
    @Mock ContentRepository contentRepository;
    @Mock RecallItemRepository recallItemRepository;
    @Mock TokenCounter tokenCounter;
    @Mock AiConfigureRepository aiConfigureRepository;
    @Mock ApplicationEventPublisher<ContentCreatedEvent> contentPublisher;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();
    private final UUID topicId = UUID.randomUUID();
    private final UUID contentId = UUID.randomUUID();
    private Content ownedContent;
    private ContentServiceImpl service;

    @BeforeEach
    void setUp() {
        User owner = User.builder().id(ownerId).build();
        Topic topic = Topic.builder().id(topicId).user(owner).build();
        ownedContent = Content.builder()
                .id(contentId)
                .topic(topic)
                .text("original text")
                .revisionDepth(RevisionDepth.BALANCED)
                .status(Status.PROCESSING_FAILED)
                .build();
        service = new ContentServiceImpl(100, topicRepository, contentRepository, recallItemRepository, tokenCounter,
                aiConfigureRepository, contentPublisher);
    }

    @Test
    void cannotCreateContentUnderAnotherUsersTopic() {
        when(topicRepository.findById(topicId)).thenReturn(Optional.of(ownedContent.getTopic()));

        assertThrows(BitRecallException.class, () -> service.createContent(topicId.toString(),
                new CreateContentRequest("new text", null), otherUserId.toString()));

        verifyNoInteractions(tokenCounter, contentPublisher);
        verify(contentRepository, never()).save(any(Content.class));
    }

    @Test
    void cannotListContentForAnotherUsersTopic() {
        when(topicRepository.findById(topicId)).thenReturn(Optional.of(ownedContent.getTopic()));

        assertThrows(BitRecallException.class,
                () -> service.findAllByTopicId(topicId.toString(), otherUserId.toString()));

        verify(contentRepository, never()).findByTopicId(topicId);
    }

    @Test
    void cannotReadAnotherUsersContent() {
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(ownedContent));

        assertThrows(BitRecallException.class,
                () -> service.findById(contentId.toString(), otherUserId.toString()));
    }

    @Test
    void cannotUpdateAnotherUsersContent() {
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(ownedContent));

        assertThrows(BitRecallException.class, () -> service.updateContent(contentId.toString(),
                Content.builder().text("attacker update").build(), otherUserId.toString()));

        verify(contentRepository, never()).update(any(Content.class));
        verifyNoInteractions(tokenCounter, contentPublisher);
    }

    @Test
    void cannotDeleteAnotherUsersContent() {
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(ownedContent));

        assertThrows(BitRecallException.class,
                () -> service.deleteContentById(contentId.toString(), otherUserId.toString()));

        verify(contentRepository, never()).delete(any(Content.class));
        verifyNoInteractions(recallItemRepository);
    }

    @Test
    void cannotRetryAnotherUsersFailedContent() {
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(ownedContent));

        assertThrows(BitRecallException.class,
                () -> service.retryContent(contentId.toString(), otherUserId.toString()));

        verify(contentRepository, never()).update(any(Content.class));
        verifyNoInteractions(contentPublisher);
    }
}
