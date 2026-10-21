package com.bit.recall.service.impl;

import com.bit.recall.ai.AIService;
import com.bit.recall.ai.AIServiceFactory;
import com.bit.recall.ai.AIProvider;
import com.bit.recall.ai.OpenAIRecallItemGenerator;
import com.bit.recall.config.ContentProcessorConfig;
import com.bit.recall.domain.Content;
import com.bit.recall.domain.RevisionDepth;
import com.bit.recall.domain.model.RecallCardDto;
import com.bit.recall.domain.model.ContentCreatedEvent;
import com.bit.recall.domain.model.RecallCardDtoWrapper;
import com.bit.recall.exception.BitRecallErrorCode;
import com.bit.recall.exception.BitRecallException;
import com.bit.recall.repo.ContentRepository;
import com.bit.recall.service.ContentProcessingService;
import com.bit.recall.service.AiConfigureService;
import com.bit.recall.service.EncryptionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Async;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;
import java.time.Instant;

import static com.bit.recall.promptsUtil.Prompts.CONTENT_PROCESSOR_PROMPT;

@Singleton
@Slf4j
@RequiredArgsConstructor
public class ContentProcessingServiceImpl implements ContentProcessingService{

    private final AIServiceFactory aiServiceFactory;
    private final OpenAIRecallItemGenerator openAIRecallItemGenerator;
    private final ContentRepository contentRepository;
    private final EncryptionService encryptionService;
    private final AiConfigureService aiConfigureService;
    private final RecallCardService recallCardService;
    private final ContentProcessorConfig config;

    @Override
    @EventListener
    @Async
    public void processContent(ContentCreatedEvent event) {
        log.info("Processing content with ID: {}", event.contentId());

        UUID contentUuid = UUID.fromString(event.contentId());
        UUID attemptId = UUID.randomUUID();
        Instant staleBefore = Instant.now().minusSeconds(config.staleAfter());
        if (contentRepository.claimForProcessing(contentUuid, attemptId, config.maxRetries(), staleBefore) != 1) {
            log.info("Skipping content {} because it is already processed, claimed, or out of retries", event.contentId());
            return;
        }
        try {
            var content = fetchContent(event.contentId());

            var preference = aiConfigureService.getPreferenceForUser(content.getTopic().getUser().getId());
            String decryptedApiKey = encryptionService.decrypt(preference.getEncryptedApiKey());
            List<RecallCardDto> recallCardDtos = processText(
                    decryptedApiKey,
                    content.getText(),
                    preference.getPreferredProvider(),
                    content.getRevisionDepth());
            recallCardService.persist(recallCardDtos, content.getTopic(), content, attemptId);
        } catch (RuntimeException | JsonProcessingException e) {
            log.error("Content processing failed for content ID: {}", event.contentId(), e);
            persistFailureStatus(contentUuid, attemptId, e);
            throw new BitRecallException(BitRecallErrorCode.AI_SERVICE_ERROR,
                    "Error while processing content ID: " + event.contentId(), e);
        }
    }

    private void persistFailureStatus(UUID contentId, UUID attemptId, Exception processingError) {
        try {
            int attempt = contentRepository.findById(contentId)
                    .map(content -> content.getProcessingAttemptCount() == null ? 1 : content.getProcessingAttemptCount())
                    .orElse(1);
            long delaySeconds = Math.min(30L * (1L << Math.min(Math.max(attempt - 1, 0), 10)), 21_600L);
            contentRepository.markAttemptFailed(contentId, attemptId, Instant.now().plusSeconds(delaySeconds));
        } catch (RuntimeException persistenceError) {
            log.error("Failed to persist PROCESSING_FAILED for content ID {}; original processing error follows",
                    contentId, persistenceError);
            processingError.addSuppressed(persistenceError);
        }
    }

    private List<RecallCardDto> processText(String apiKey, String text, AIProvider provider, RevisionDepth revisionDepth) {
        String userPrompt = """
            Revision Depth: %s

            SOURCE MATERIAL:
            %s
            """.formatted(revisionDepth.name(), text);

        if (provider == AIProvider.OPENAI) {
            return openAIRecallItemGenerator.generateRecallItems(
                    apiKey, CONTENT_PROCESSOR_PROMPT, userPrompt);
        }

        AIService aiService = aiServiceFactory.getService(provider);
        var processedData = aiService.generateStructuredOutput(apiKey, CONTENT_PROCESSOR_PROMPT, userPrompt, RecallCardDtoWrapper.class);
        return processedData.recallCards();
    }

    private Content fetchContent(String contentId) {
        return contentRepository.findById(UUID.fromString(contentId))
                .orElseThrow(() -> new BitRecallException(BitRecallErrorCode.NOT_FOUND, "Topic not found with id: " + contentId));

    }
}
