package com.bit.recall.service.impl;


import com.bit.recall.domain.Content;
import com.bit.recall.domain.model.ContentCreatedEvent;
import com.bit.recall.domain.model.ContentResponse;
import com.bit.recall.domain.model.CreateContentRequest;
import com.bit.recall.domain.Topic;
import com.bit.recall.repo.ContentRepository;
import com.bit.recall.repo.TopicRepository;
import com.bit.recall.service.ContentService;
import io.micronaut.context.event.ApplicationEventPublisher;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class ContentServiceImpl implements ContentService {

    private final TopicRepository topicRepository;
    private final ContentRepository contentRepository;
    private final ApplicationEventPublisher<ContentCreatedEvent> contentPublisher;

    @Override
    public Content createContent(String topicId, CreateContentRequest createContentRequest) {
        Topic topic = findTopicByID(topicId);
        if (topic == null) {
            throw new NoSuchElementException("Topic not found with id: %s ,Create topicId First and add contents".formatted(topicId));
        }
        Content content = Content.builder()
                .id(UUID.randomUUID())
                .text(createContentRequest.text())
                .topic(topic)
                .createdAt(Instant.now())
                .build();
        contentRepository.save(content);
        log.info("Content created with id: {} for topicId: {}", content.getId(), topicId);
        publishEvent(content);
        return content;
    }

    @Override
    public Optional<Content> findById(String contentId) {
        return Optional.ofNullable(contentRepository.findById(UUID.fromString(contentId)))
                .orElseThrow(() -> new NoSuchElementException("Content not found with id: " + contentId));
    }

    @Override
    public List<ContentResponse> findAllByTopicId(String topicId) {
        try {
            UUID topicUuid = UUID.fromString(topicId);
            return contentRepository.findByTopicId(topicUuid).stream()
                    .map(content -> ContentResponse.builder()
                            .id(content.getId().toString())
                            .text(content.getText())
                            .createdAt(content.getCreatedAt())
                            .build())
                    .collect(toList());
        } catch (IllegalArgumentException e) {
            // Invalid UUID string passed
            log.error("Invalid topicId UUID format: {}", topicId);
            return List.of();
        }
    }

    @Override
    public void deleteContentById( String contentId) {
       contentRepository.findById(UUID.fromString(contentId))
                .ifPresentOrElse(contentRepository::delete, () -> {
                    throw new NoSuchElementException("Content not found with id: " + contentId);
                });
    }

    @Override
    public Content updateContent(String contentId, Content updateableContent) {
        contentRepository.findById(UUID.fromString(contentId))
                .ifPresentOrElse(existingContent -> {
                    existingContent.setText(updateableContent.getText());
                    contentRepository.update(existingContent);
                }, () -> {
                    throw new NoSuchElementException("Content not found with id: " + contentId);
                });

        return updateableContent;
    }

    private void publishEvent(Content content) {
        ContentCreatedEvent event = ContentCreatedEvent.builder()
                .id(content.getId().toString())
                .topicId(content.getTopic().getId().toString())
                .contentId(content.getId().toString())
                .text(content.getText())
                .createdAt(content.getCreatedAt())
                .build();
        contentPublisher.publishEvent(event);
        log.info("Published ContentCreatedEvent for contentId: {}", content.getId());
    }

    private Topic findTopicByID(String topicId) {
        return topicRepository.findById(UUID.fromString(topicId))
                .orElseThrow(() -> new NoSuchElementException("Topic not found with id: " + topicId));
    }
}

