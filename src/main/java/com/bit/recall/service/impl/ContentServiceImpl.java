package com.bit.recall.service.impl;


import com.bit.recall.domain.Content;
import com.bit.recall.domain.RevisionDepth;
import com.bit.recall.domain.Topic;
import com.bit.recall.domain.model.ContentCreatedEvent;
import com.bit.recall.domain.model.ContentResponse;
import com.bit.recall.domain.model.CreateContentRequest;
import com.bit.recall.repo.AiConfigureRepository;
import com.bit.recall.repo.ContentRepository;
import com.bit.recall.repo.TopicRepository;
import com.bit.recall.service.ContentService;
import com.bit.recall.utils.TokenCounter;
import io.micronaut.context.annotation.Value;
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

    @Value("${bit-recall.content.max-tokens: 20000}")
    private final int MAX_TOKEN_LIMIT;

    private final TopicRepository topicRepository;
    private final ContentRepository contentRepository;
    private final TokenCounter tokenCounter;
    private final AiConfigureRepository aiConfigureRepository;
    private final ApplicationEventPublisher<ContentCreatedEvent> contentPublisher;

    @Override
    public Content createContent(String topicId, CreateContentRequest createContentRequest, String userId) {
        // Validate ownership of the topic before proceeding
        var topic = validateOwnerShip(topicId, userId).get();

        //count tokens before creating content
        var tokenCount = tokenCounter.countTokens(createContentRequest.text());
        if(tokenCount > MAX_TOKEN_LIMIT) {
            throw new IllegalArgumentException("Content exceeds maximum token limit of " + MAX_TOKEN_LIMIT + ". Current token count: " + tokenCount);
        }

        var revisionDepth = resolveRevisionDepth(createContentRequest.revisionDepth());

        Content content = Content.builder()
                .id(UUID.randomUUID())
                .text(createContentRequest.text())
                .topic(topic)
                .createdAt(Instant.now())
                .revisionDepth(revisionDepth)
                .build();
        contentRepository.save(content);
        log.info("Content created with id: {} for topicId: {}", content.getId(), topicId);
        publishEvent(content, userId);
        return content;
    }

    @Override
    public Optional<Content> findById(String contentId, String userId) {
        Content content = contentRepository.findById(UUID.fromString(contentId))
                .orElseThrow(() ->
                        new NoSuchElementException("Content not found"));

        if (!content.getTopic().getUser().getId().equals(UUID.fromString(userId))) {
            throw new NoSuchElementException("You don't have access to this content");
        }
        return Optional.ofNullable(contentRepository.findById(UUID.fromString(contentId)))
                .orElseThrow(() -> new NoSuchElementException("Content not found with id: " + contentId));
    }

    @Override
    public List<ContentResponse> findAllByTopicId(String topicId, String userId) {
        try {
            // Validate ownership of the topic before proceeding
            var topic = validateOwnerShip(topicId, userId).get();

            return contentRepository.findByTopicId(UUID.fromString(topicId)).stream()
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
    public void deleteContentById( String contentId, String userId) {
        Content content = contentRepository.findById(UUID.fromString(contentId))
                .orElseThrow(() ->
                        new NoSuchElementException("Content not found"));

        if (!content.getTopic().getUser().getId().equals(UUID.fromString(userId))) {
            throw new NoSuchElementException("You don't have access to this content");
        }
       contentRepository.findById(UUID.fromString(contentId))
                .ifPresentOrElse(contentRepository::delete, () -> {
                    throw new NoSuchElementException("Content not found with id: " + contentId);
                });
    }

    @Override
    public Content updateContent(String contentId, Content updateableContent, String userId) {
        Content content = contentRepository.findById(UUID.fromString(contentId))
                .orElseThrow(() ->
                        new NoSuchElementException("Content not found"));

        if (!content.getTopic().getUser().getId().equals(UUID.fromString(userId))) {
            throw new NoSuchElementException("You don't have access to this content");
        }
        contentRepository.findById(UUID.fromString(contentId))
                .ifPresentOrElse(existingContent -> {
                    existingContent.setText(updateableContent.getText());
                    contentRepository.update(existingContent);
                }, () -> {
                    throw new NoSuchElementException("Content not found with id: " + contentId);
                });

        return updateableContent;
    }

    private Optional<Topic> validateOwnerShip(String topicID, String userId) {
        var topic = topicRepository.findById(UUID.fromString(topicID))
                .orElseThrow(() -> new NoSuchElementException("Topic not found with id: " + topicID));
        if (!topic.getUser().getId().toString().equals(userId)) {
            throw new IllegalArgumentException("User does not have permission to access this topic.");
        }
        return Optional.of(topic);
    }

    private RevisionDepth resolveRevisionDepth(String revisionDepth) {
        if (revisionDepth == null || revisionDepth.isEmpty()) {
            return RevisionDepth.COMPREHENSIVE; // default value
        }
        try {
            return RevisionDepth.valueOf(revisionDepth.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid revision depth: " + revisionDepth);
        }
    }

    private void publishEvent(Content content, String userId) {
        var userAiPreference = aiConfigureRepository.findByUserId(UUID.fromString(userId))
                .orElseThrow(() -> new IllegalStateException("AI Provider API Key not configured. Please configure in settings."));

        ContentCreatedEvent event = ContentCreatedEvent.builder()
                .id(content.getId().toString())
                .topicId(content.getTopic().getId().toString())
                .contentId(content.getId().toString())
                .text(content.getText())
                .revisionDepth(content.getRevisionDepth())
                .aiProvider(userAiPreference.getPreferredProvider())
                .apiKey(userAiPreference.getEncryptedApiKey())
                .createdAt(content.getCreatedAt())
                .build();
        contentPublisher.publishEvent(event);
        log.info("Published ContentCreatedEvent for contentId: {}", content.getId());
    }
}

