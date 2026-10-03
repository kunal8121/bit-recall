package com.bit.recall.service.impl;

import com.bit.recall.domain.Content;
import com.bit.recall.domain.RecallItem;
import com.bit.recall.domain.Topic;
import com.bit.recall.domain.model.RecallCardDto;
import com.bit.recall.repo.ContentRepository;
import com.bit.recall.repo.RecallItemRepository;
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

    @Transactional
    public void persist(List<RecallCardDto> recallCards, Topic topic, Content content) {
        for (RecallCardDto recallDto : recallCards) {
            RecallItem recallItem = RecallItem.builder()
                    .id(UUID.randomUUID())
                    .title(recallDto.title())
                    .question(recallDto.recall())
                    .summaryBody(recallDto.explanation())
                    .topic(topic)
                    .content(content)
                    .build();

            recallItemRepository.save(recallItem);
            log.debug("Persisted RecallItem with ID: {}, Question: {}, Topic ID: {}",
                    recallItem.getId(), recallItem.getQuestion(), topic.getId());
        }

        contentRepository.update(content.toBuilder().status(PROCESSED).build());
    }
}
