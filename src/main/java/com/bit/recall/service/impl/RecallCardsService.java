package com.bit.recall.service.impl;

import com.bit.recall.domain.Content;
import com.bit.recall.domain.RecallItem;
import com.bit.recall.domain.Topic;
import com.bit.recall.domain.model.CursorData;
import com.bit.recall.domain.model.CursorResponse;
import com.bit.recall.domain.model.RecallCardDto;
import com.bit.recall.domain.model.RecallCardResponse;
import com.bit.recall.domain.model.RecallMetaDataDto;
import com.bit.recall.exception.BitRecallErrorCode;
import com.bit.recall.exception.BitRecallException;
import com.bit.recall.mapper.RestRecallCardMapper;
import com.bit.recall.repo.ContentRepository;
import com.bit.recall.repo.RecallItemRepository;
import com.bit.recall.repo.TopicRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor
@Slf4j
public class RecallCardsService {

    private final RecallItemRepository recallItemRepository;
    private final ContentRepository contentRepository;
    private final TopicRepository topicRepository;
    private final RestRecallCardMapper recallCardMapper;

    private final ObjectMapper mapper = new ObjectMapper();

    @Transactional
    public void persist(List<RecallCardDto> recallCards, Topic topic, Content content, UUID attemptId) throws JsonProcessingException {
        // Replace cards only after generation succeeded; the transaction keeps the old set if persistence fails.
        recallItemRepository.deleteAll(recallItemRepository.findByContentId(content.getId()));
        for (RecallCardDto recallDto : recallCards) {

            RecallMetaDataDto metaData = RecallMetaDataDto.builder()
                    .type(recallDto.type())
                    .example(recallDto.example())
                    .pattern(recallDto.pattern())
                    .code(recallDto.code())
                    .build();

            RecallItem recallItem = RecallItem.builder()
                    .id(UUID.randomUUID())
                    .title(recallDto.title())
                    .question(recallDto.recall())
                    .summaryBody(recallDto.explanation())
                    .topic(topic)
                    .content(content)
                    .metaData(mapper.writeValueAsString(metaData))
                    .build();

            recallItemRepository.save(recallItem);
            log.debug("Persisted RecallItem with ID: {}, Question: {}, Topic ID: {}",
                    recallItem.getId(), recallItem.getQuestion(), topic.getId());
        }

        if (contentRepository.markAttemptProcessed(content.getId(), attemptId) != 1) {
            throw new IllegalStateException("Processing attempt no longer owns content " + content.getId());
        }
    }

    public CursorResponse<RecallCardResponse> fetchRecallCardsByTopicId(UUID topicId,
                                                                        UUID userId,
                                                                        int pageSize,
                                                                        String cursor) {
        topicRepository.findByIdAndUserId(topicId, userId)
                .orElseThrow(() -> new BitRecallException(BitRecallErrorCode.NOT_FOUND,
                        "Topic not found with id: " + topicId));

        CursorData cursorData = cursor == null || cursor.isBlank()
                ? null
                : CursorData.decodeCursor(cursor);
        List<RecallItem> recallItems = recallItemRepository.findByTopicId(
                topicId,
                userId,
                pageSize + 1,
                cursorData == null ? null : cursorData.lastCreatedAt(),
                cursorData == null ? null : cursorData.entityId());

        boolean hasNextPage = recallItems.size() > pageSize;
        if (hasNextPage) {
            recallItems = recallItems.subList(0, pageSize);
        }

        String nextCursor = null;
        if (hasNextPage && !recallItems.isEmpty()) {
            RecallItem lastItem = recallItems.getLast();
            nextCursor = CursorData.encodeCursor(new CursorData(lastItem.getCreatedAt(), lastItem.getId()));
        }

        return CursorResponse.<RecallCardResponse>builder()
                .pageSize(pageSize)
                .hasNext(hasNextPage)
                .nextCursor(nextCursor)
                .data(recallItems.stream().map(recallCardMapper::toRecallCardResponse).toList())
                .build();
    }
}
