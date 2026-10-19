package com.bit.recall.service.impl;

import com.bit.recall.ai.AIService;
import com.bit.recall.ai.AIServiceFactory;
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
import io.micronaut.context.annotation.Value;
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
    private final ContentRepository contentRepository;
    private final EncryptionService encryptionService;
    private final AiConfigureService aiConfigureService;
    private final RecallCardService recallCardService;

    @Value("${bit-recall.processing.max-retries:3}")
    private int maxRetries;

    @Value("${bit-recall.processing.stale-after-seconds:600}")
    private int staleAfterSeconds;

    @Override
    @EventListener
    @Async
    public void processContent(ContentCreatedEvent event) {
        log.info("Processing content with ID: {}", event.contentId());

        UUID contentUuid = UUID.fromString(event.contentId());
        UUID attemptId = UUID.randomUUID();
        Instant staleBefore = Instant.now().minusSeconds(staleAfterSeconds);
        if (contentRepository.claimForProcessing(contentUuid, attemptId, maxRetries, staleBefore) != 1) {
            log.info("Skipping content {} because it is already processed, claimed, or out of retries", event.contentId());
            return;
        }
        try {
            var content = fetchContent(event.contentId());

            var preference = aiConfigureService.getPreferenceForUser(content.getTopic().getUser().getId());
            AIService aiService = aiServiceFactory.getService(preference.getPreferredProvider());
            String decryptedApiKey = encryptionService.decrypt(preference.getEncryptedApiKey());
            List<RecallCardDto> recallCardDtos = processText(decryptedApiKey, content.getText(), aiService, content.getRevisionDepth());
            recallCardService.persist(recallCardDtos, content.getTopic(), content, attemptId);
        } catch (RuntimeException | JsonProcessingException e) {
            log.error("Content processing failed for content ID: {}", event.contentId(), e);
            persistFailureStatus(contentUuid, attemptId, e);
            throw new BitRecallException(BitRecallErrorCode.AI_SERVICE_ERROR,
                    "Error while processing content ID: " + event.contentId(), e);
        }
    }

//    /** On restart, an in-memory async event may have been lost. Convert abandoned work to retryable failure. */
//    @EventListener
//    public void recoverInterruptedProcessing(StartupEvent event) {
//        List<Content> interrupted = contentRepository.findByStatus(PROCESSING, null, null);
//        for (Content content : interrupted) {
//            try {
//                recallCardService.findRecallItemsByContentId(content.getId());
//                contentRepository.update(content.toBuilder().status(PROCESSING_FAILED).build());
//                log.warn("Marked interrupted content {} as PROCESSING_FAILED during startup recovery", content.getId());
//            } catch (RuntimeException e) {
//                log.error("Could not recover interrupted content {} during startup", content.getId(), e);
//            }
//        }
//    }

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

    private List<RecallCardDto> processText(String apiKey, String text, AIService aiService, RevisionDepth revisionDepth) {
        String userPrompt = """
            Revision Depth: %s

            SOURCE MATERIAL:
            %s
            """.formatted(revisionDepth.name(), text);
        var processedData = aiService.generateStructuredOutput(apiKey, CONTENT_PROCESSOR_PROMPT, userPrompt, RecallCardDtoWrapper.class);
        return processedData.recallCards();
    }

    private Content fetchContent(String contentId) {
        return contentRepository.findById(UUID.fromString(contentId))
                .orElseThrow(() -> new BitRecallException(BitRecallErrorCode.NOT_FOUND, "Topic not found with id: " + contentId));

    }
}
