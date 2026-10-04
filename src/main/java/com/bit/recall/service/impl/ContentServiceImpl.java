package com.bit.recall.service.impl;


import com.bit.recall.domain.Content;
import com.bit.recall.domain.RevisionDepth;
import com.bit.recall.domain.Topic;
import com.bit.recall.domain.UserAiPreference;
import com.bit.recall.domain.model.ContentCreatedEvent;
import com.bit.recall.domain.model.ContentResponse;
import com.bit.recall.domain.model.CreateContentRequest;
import com.bit.recall.exception.BitRecallErrorCode;
import com.bit.recall.exception.BitRecallException;
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
            throw new BitRecallException(BitRecallErrorCode.BAD_REQUEST, "Content exceeds maximum token limit of " + MAX_TOKEN_LIMIT + ". Current token count: " + tokenCount);
        }

        var revisionDepth = resolveRevisionDepth(createContentRequest.revisionDepth());
        var userAiPreference = fetchAiProviderConfiguration(userId);

        Content content = Content.builder()
                .id(UUID.randomUUID())
                .text(createContentRequest.text())
                .topic(topic)
                .createdAt(Instant.now())
                .revisionDepth(revisionDepth)
                .build();
        contentRepository.save(content);
        log.info("Content created with id: {} for topicId: {}", content.getId(), topicId);
        publishEvent(content, userAiPreference);
        return content;
    }

    private UserAiPreference fetchAiProviderConfiguration(String userId) {
        return aiConfigureRepository.findByUserId(UUID.fromString(userId))
                .orElseThrow(() -> new BitRecallException(BitRecallErrorCode.INVALID_API_KEY,
                        "AI provider API key is not configured. Please configure it in settings."));
    }

    @Override
    public Optional<Content> findById(String contentId, String userId) {
        Content content = contentRepository.findById(UUID.fromString(contentId))
                .orElseThrow(() ->
                        new BitRecallException(BitRecallErrorCode.NOT_FOUND, "Content not found"));

        if (!content.getTopic().getUser().getId().equals(UUID.fromString(userId))) {
            throw new BitRecallException(BitRecallErrorCode.FORBIDDEN, "You don't have access to this content");
        }
        return Optional.of(content);
    }

    @Override
    public List<ContentResponse> findAllByTopicId(String topicId, String userId) {
        UUID topicUUID;
        try {
            topicUUID = UUID.fromString(topicId);
        } catch (IllegalArgumentException e) {
            throw new BitRecallException(BitRecallErrorCode.BAD_REQUEST, "Invalid topicId UUID format", e);
        }

        // Validate ownership of the topic before proceeding.
        validateOwnerShip(topicUUID.toString(), userId);
        return contentRepository.findByTopicId(topicUUID).stream()
                .map(content -> ContentResponse.builder()
                        .id(content.getId().toString())
                        .text(content.getText())
                        .createdAt(content.getCreatedAt())
                        .build())
                .collect(toList());
    }

    @Override
    public void deleteContentById( String contentId, String userId) {
        Content content = contentRepository.findById(UUID.fromString(contentId))
                .orElseThrow(() ->
                        new BitRecallException(BitRecallErrorCode.NOT_FOUND, "Content not found"));

        if (!content.getTopic().getUser().getId().equals(UUID.fromString(userId))) {
            throw new BitRecallException(BitRecallErrorCode.FORBIDDEN, "You don't have access to this content");
        }
        contentRepository.delete(content);
    }

    @Override
    public Content updateContent(String contentId, Content updateableContent, String userId) {
        Content content = contentRepository.findById(UUID.fromString(contentId))
                .orElseThrow(() -> new BitRecallException(BitRecallErrorCode.NOT_FOUND, "Content not found"));

        if (!content.getTopic().getUser().getId().equals(UUID.fromString(userId))) {
            throw new BitRecallException(BitRecallErrorCode.FORBIDDEN, "You don't have access to this content");
        }
        if (content.getStatus() == Content.Status.PROCESSING) {
            throw new BitRecallException(BitRecallErrorCode.CONFLICT, "Content is already being processed");
        }
        int tokenCount = tokenCounter.countTokens(updateableContent.getText());
        if (tokenCount > MAX_TOKEN_LIMIT) {
            throw new BitRecallException(BitRecallErrorCode.BAD_REQUEST, "Content exceeds maximum token limit of " + MAX_TOKEN_LIMIT
                    + ". Current token count: " + tokenCount);
        }

        RevisionDepth revisionDepth = updateableContent.getRevisionDepth() == null
                ? content.getRevisionDepth() : updateableContent.getRevisionDepth();
        UserAiPreference preference = fetchAiProviderConfiguration(userId);
        content.setText(updateableContent.getText());
        content.setRevisionDepth(revisionDepth);
        content.setStatus(Content.Status.PROCESSING);
        contentRepository.update(content);
        publishEvent(content, preference);
        return content;
    }

    @Override
    public Content retryContent(String contentId, String userId) {
        Content content = contentRepository.findById(UUID.fromString(contentId))
                .orElseThrow(() -> new BitRecallException(BitRecallErrorCode.NOT_FOUND, "Content not found"));
        if (!content.getTopic().getUser().getId().equals(UUID.fromString(userId))) {
            throw new BitRecallException(BitRecallErrorCode.FORBIDDEN, "You don't have access to this content");
        }
        if (content.getStatus() != Content.Status.PROCESSING_FAILED) {
            throw new BitRecallException(BitRecallErrorCode.CONFLICT, "Only failed content can be retried");
        }
        UserAiPreference preference = fetchAiProviderConfiguration(userId);
        content.setStatus(Content.Status.PROCESSING);
        contentRepository.update(content);
        publishEvent(content, preference);
        return content;
    }

    private Optional<Topic> validateOwnerShip(String topicID, String userId) {
        var topic = topicRepository.findById(UUID.fromString(topicID))
                .orElseThrow(() -> new BitRecallException(BitRecallErrorCode.NOT_FOUND, "Topic not found with id: " + topicID));
        if (!topic.getUser().getId().toString().equals(userId)) {
            throw new BitRecallException(BitRecallErrorCode.FORBIDDEN, "User does not have permission to access this topic.");
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
            throw new BitRecallException(BitRecallErrorCode.BAD_REQUEST, "Invalid revision depth: " + revisionDepth, e);
        }
    }

    private void publishEvent(Content content, UserAiPreference userAiPreference) {
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

