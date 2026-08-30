package com.bit.recall.service.impl;

import com.bit.recall.domain.Content;
import com.bit.recall.domain.Topic;
import com.bit.recall.domain.User;
import com.bit.recall.domain.model.CreateTopicRequest;
import com.bit.recall.repo.ContentRepository;
import com.bit.recall.repo.TopicRepository;
import com.bit.recall.service.TopicService;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.http.server.exceptions.NotFoundException;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class TopicServiceImpl implements TopicService {

    private final TopicRepository topicRepository;
    private final ContentRepository contentRepository;

    @Transactional
    @Override
    public Topic createTopicWithUser(CreateTopicRequest request, User user) {
        Topic topic = Topic.builder()
                .id(UUID.randomUUID())
                .title(request.title())
                .description(request.description())
                .user(user)
                .build();

        if (CollectionUtils.isNotEmpty(request.contents())) {
            ArrayList<Content> contentList = new ArrayList<>();

            request.contents().forEach(contentRequest -> {
                Content content = Content.builder()
                        .id(UUID.randomUUID())
                        .text(contentRequest.text ())
                        .topic(topic)
                        .createdAt(Instant.now())
                        .build();

                contentRepository.save(content);
                contentList.add(content);
            });

            topic.setContents(contentList);
        }
        try {
            return topicRepository.save(topic);
        } catch (Exception e) {
            log.error("Error saving topicId: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Topic getTopicById(String id) {
        return topicRepository.findById(UUID.fromString(id)).orElseThrow(() -> new NoSuchElementException("Topic not found with id: " + id));
    }

    @Override
    public List<Topic> findAll() {
        return topicRepository.findAll();
    }

    @Override
    public void deleteTopicById(String id) {
        UUID topicUuid = UUID.fromString(id);

        Topic topic = topicRepository.findById(topicUuid)
                .orElseThrow(NotFoundException::new);

        topicRepository.delete(topic);
    }

    @Override
    public Topic updateTopic(String id, Topic topic) {
        Topic existingTopic = getTopicById(id);
        existingTopic.setTitle(topic.getTitle());
        existingTopic.setDescription(topic.getDescription());
        return topicRepository.save(existingTopic);
    }
}


