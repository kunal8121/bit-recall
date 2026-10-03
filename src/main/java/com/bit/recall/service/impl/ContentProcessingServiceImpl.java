package com.bit.recall.service.impl;

import com.bit.recall.ai.AIService;
import com.bit.recall.ai.AIProvider;
import com.bit.recall.ai.AIServiceFactory;
import com.bit.recall.domain.Content;
import com.bit.recall.domain.RecallItem;
import com.bit.recall.domain.RevisionDepth;
import com.bit.recall.domain.Topic;
import com.bit.recall.domain.model.RecallCardDto;
import com.bit.recall.domain.model.ContentCreatedEvent;
import com.bit.recall.domain.model.RecallCardDtoWrapper;
import com.bit.recall.repo.ContentRepository;
import com.bit.recall.repo.RecallItemRepository;
import com.bit.recall.repo.TopicRepository;
import com.bit.recall.service.ContentProcessingService;
import com.bit.recall.service.EncryptionService;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Async;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.bit.recall.domain.Content.Status.*;
import static com.bit.recall.promptsUtil.Prompts.BASIC_USER_PROMPT;
import static com.bit.recall.promptsUtil.Prompts.CONTENT_PROCESSOR_PROMPT;

@Singleton
@Slf4j
@RequiredArgsConstructor
public class ContentProcessingServiceImpl implements ContentProcessingService{

    private final AIServiceFactory aiServiceFactory;
    private final TopicRepository topicRepository;
    private final RecallItemRepository recallItemRepository;
    private final ContentRepository contentRepository;
    private final EncryptionService encryptionService;

    @Override
    @EventListener
    @Async
    @Transactional
    public void processContent(ContentCreatedEvent event) {
        log.info("Processing content with ID: {}, Topic ID: {}, Text: {}", event.id(), event.topicId(), event.text());

        try {
            var content = fetchContent(event.contentId()).get();
            updateContentStatus(content, PROCESSING);

            AIProvider preferredAiProvider = event.aiProvider() == null ? AIProvider.OPENAPI : event.aiProvider();
            AIService aiService = aiServiceFactory.getService(preferredAiProvider);
            String decryptedApiKey = encryptionService.decrypt(event.apiKey());
            List<RecallCardDto> recallCardDtos = processText(decryptedApiKey, event.text(), aiService, event.revisionDepth());
            persistRecallItem(recallCardDtos, fetchTopic(event.topicId()).get(), content);
        } catch (RuntimeException e) {
            throw new RuntimeException("Error while processing content event",  e);
        }
    }

    private List<RecallCardDto> processText(String apiKey, String text, AIService aiService, RevisionDepth revisionDepth) {
        var userPrompt = BASIC_USER_PROMPT + "\n" + "Revision Depth: " + revisionDepth.name();
        var systemPrompt = CONTENT_PROCESSOR_PROMPT + "\n" + text;
        var processedData = aiService.generateStructuredOutput(apiKey, systemPrompt, BASIC_USER_PROMPT, RecallCardDtoWrapper.class);
        return processedData.recallCards();
    }

    private void persistRecallItem(List<RecallCardDto> recallCards , Topic topic, Content content) {
        try {
            recallCards.forEach(recallDto -> {
                RecallItem recallItem = RecallItem.builder()
                        .id(UUID.randomUUID())
                        .title(recallDto.title())
                        .question(recallDto.question())
                        .summaryBody(recallDto.answer())
                        .topic(topic)
                        .content(content)
                        .build();

                recallItemRepository.save(recallItem);
                log.info("Persisted RecallItem with ID: {}, Question: {}, Answer: {}, Topic ID: {}",
                        recallItem.getId(), recallItem.getQuestion(), recallItem.getSummaryBody(), recallItem.getTopic().getId());
            });
            updateContentStatus(content, PROCESSED);
        } catch (Exception e) {
            updateContentStatus(content, PROCESSING_FAILED);
            log.error("Error while persisting recall items for content ID: {}. Marked as PROCESSING_FAILED.", content.getId(), e);
        }
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
