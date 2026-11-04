package com.bit.recall.service.impl;

import com.bit.recall.domain.Topic;
import com.bit.recall.domain.User;
import com.bit.recall.domain.model.CreateTopicRequest;
import com.bit.recall.exception.BitRecallException;
import com.bit.recall.repo.ContentRepository;
import com.bit.recall.repo.TopicRepository;
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
class TopicServiceImplTest {
    @Mock TopicRepository topicRepository;
    @Mock ContentRepository contentRepository;

    @Test
    void createsTopicForUser() {
        when(topicRepository.save(any(Topic.class))).thenAnswer(invocation -> invocation.getArgument(0));
        User user = User.builder().id(UUID.randomUUID()).build();
        TopicServiceImpl service = new TopicServiceImpl(topicRepository, contentRepository);

        Topic topic = service.createTopicWithUser(new CreateTopicRequest("Math", "desc"), user);

        assertEquals("Math", topic.getTitle());
        assertEquals("desc", topic.getDescription());
        assertSame(user, topic.getUser());
        verify(topicRepository).save(topic);
    }

    @Test
    void fetchesAndDeletesTopicOnlyForOwner() {
        UUID topicId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Topic topic = Topic.builder().id(topicId).build();
        when(topicRepository.findByIdAndUserId(topicId, userId)).thenReturn(Optional.of(topic));
        TopicServiceImpl service = new TopicServiceImpl(topicRepository, contentRepository);

        assertSame(topic, service.getTopicById(topicId.toString(), userId.toString()));
        service.deleteTopicById(topicId.toString(), userId.toString());
        verify(topicRepository).delete(topic);

        when(topicRepository.findByIdAndUserId(topicId, userId)).thenReturn(Optional.empty());
        assertThrows(BitRecallException.class,
                () -> service.getTopicById(topicId.toString(), userId.toString()));
    }

    @Test
    void updatesTopicAndListsByUser() {
        UUID topicId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Topic existing = Topic.builder().id(topicId).title("Old").description("Old description").build();
        when(topicRepository.findByIdAndUserId(topicId, userId)).thenReturn(Optional.of(existing));
        when(topicRepository.save(existing)).thenReturn(existing);
        TopicServiceImpl service = new TopicServiceImpl(topicRepository, contentRepository);

        Topic updated = service.updateTopic(topicId.toString(),
                Topic.builder().title("New").description("New description").build(), userId.toString());
        service.findAll(userId.toString());

        assertEquals("New", updated.getTitle());
        assertEquals("New description", updated.getDescription());
        verify(topicRepository).findAllByUserId(userId);
    }
}
