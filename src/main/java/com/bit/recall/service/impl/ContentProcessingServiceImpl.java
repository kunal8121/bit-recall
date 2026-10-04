package com.bit.recall.service.impl;

import com.bit.recall.ai.AIService;
import com.bit.recall.ai.AIProvider;
import com.bit.recall.ai.AIServiceFactory;
import com.bit.recall.domain.Content;
import com.bit.recall.domain.RevisionDepth;
import com.bit.recall.domain.Topic;
import com.bit.recall.domain.model.RecallCardDto;
import com.bit.recall.domain.model.ContentCreatedEvent;
import com.bit.recall.domain.model.RecallCardDtoWrapper;
import com.bit.recall.exception.BitRecallErrorCode;
import com.bit.recall.exception.BitRecallException;
import com.bit.recall.repo.ContentRepository;
import com.bit.recall.repo.TopicRepository;
import com.bit.recall.service.ContentProcessingService;
import com.bit.recall.service.EncryptionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.scheduling.annotation.Async;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.bit.recall.domain.Content.Status.*;
import static com.bit.recall.promptsUtil.Prompts.CONTENT_PROCESSOR_PROMPT;

@Singleton
@Slf4j
@RequiredArgsConstructor
public class ContentProcessingServiceImpl implements ContentProcessingService{

    private final AIServiceFactory aiServiceFactory;
    private final TopicRepository topicRepository;
    private final ContentRepository contentRepository;
    private final EncryptionService encryptionService;
    private final RecallCardPersistenceService recallCardPersistenceService;

    @Override
    @EventListener
    @Async
    public void processContent(ContentCreatedEvent event) {
        log.info("Processing content with ID: {}, Topic ID: {}", event.contentId(), event.topicId());

        try {
            var content = fetchContent(event.contentId()).get();
            updateContentStatus(content, PROCESSING);

            AIProvider preferredAiProvider = event.aiProvider() == null ? AIProvider.OPENAI : event.aiProvider();
            AIService aiService = aiServiceFactory.getService(preferredAiProvider);
            String decryptedApiKey = encryptionService.decrypt(event.apiKey());
            List<RecallCardDto> recallCardDtos = processText(decryptedApiKey, event.text(), aiService, event.revisionDepth());
            recallCardPersistenceService.persist(recallCardDtos, fetchTopic(event.topicId()).get(), content);
        } catch (RuntimeException | JsonProcessingException e) {
            log.error("Content processing failed for content ID: {}", event.contentId(), e);
            persistFailureStatus(event.contentId(), e);
            throw new BitRecallException(BitRecallErrorCode.AI_SERVICE_ERROR,
                    "Error while processing content ID: " + event.contentId(), e);
        }
    }

    /** On restart, an in-memory async event may have been lost. Convert abandoned work to retryable failure. */
    @EventListener
    public void recoverInterruptedProcessing(StartupEvent event) {
        List<Content> interrupted = contentRepository.findByStatus(PROCESSING);
        for (Content content : interrupted) {
            try {
                contentRepository.update(content.toBuilder().status(PROCESSING_FAILED).build());
                log.warn("Marked interrupted content {} as PROCESSING_FAILED during startup recovery", content.getId());
            } catch (RuntimeException e) {
                log.error("Could not recover interrupted content {} during startup", content.getId(), e);
            }
        }
    }

    private void persistFailureStatus(String contentId, Exception processingError) {
        try {
            UUID id = UUID.fromString(contentId);
            contentRepository.findById(id).ifPresentOrElse(
                    content -> contentRepository.update(content.toBuilder().status(PROCESSING_FAILED).build()),
                    () -> log.error("Cannot persist PROCESSING_FAILED for content ID {}: content row not found", contentId));
        } catch (RuntimeException persistenceError) {
            log.error("Failed to persist PROCESSING_FAILED for content ID {}; original processing error follows",
                    contentId, persistenceError);
            processingError.addSuppressed(persistenceError);
        }
    }

    private List<RecallCardDto> processText(String apiKey, String text, AIService aiService, RevisionDepth revisionDepth) {
        String userPrompt = """
            Revision Depth: %s

            SOURCE MATERIAL:
            %s
            """.formatted(revisionDepth.name(), text);
        var processedData = aiService.generateStructuredOutput(apiKey, CONTENT_PROCESSOR_PROMPT, userPrompt, RecallCardDtoWrapper.class);
        return processedData.recallCards();
    }

    private void updateContentStatus(Content content, Content.Status status) {
        var processedContent = content.toBuilder().status(status).build();
        contentRepository.update(processedContent);
    }


    private Optional<Topic> fetchTopic(String topicId) {
        return Optional.ofNullable(topicRepository.findById(UUID.fromString(topicId)))
                .orElseThrow(() -> new IllegalArgumentException("Topic not found with id: " + topicId));
    }

    private Optional<Content> fetchContent(String contentId) {
        return Optional.ofNullable(contentRepository.findById(UUID.fromString(contentId)))
                .orElseThrow(() -> new IllegalArgumentException("Content not found with id: " + contentId));
    }
}
