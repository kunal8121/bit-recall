package com.bit.recall.service.impl;

import com.bit.recall.domain.Content;
import com.bit.recall.domain.RecallItem;
import com.bit.recall.domain.Topic;
import com.bit.recall.domain.model.RecallCardDto;
import com.bit.recall.domain.model.RecallMetaDataDto;
import com.bit.recall.repo.ContentRepository;
import com.bit.recall.repo.RecallItemRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

import static com.bit.recall.domain.Content.Status.PROCESSED;

@Singleton
@RequiredArgsConstructor
@Slf4j
public class RecallCardPersistenceService {

    private final RecallItemRepository recallItemRepository;
    private final ContentRepository contentRepository;

    ObjectMapper mapper = new ObjectMapper();

    @Transactional
    public void persist(List<RecallCardDto> recallCards, Topic topic, Content content) throws JsonProcessingException {
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

        contentRepository.update(content.toBuilder().status(PROCESSED).build());
    }
}
