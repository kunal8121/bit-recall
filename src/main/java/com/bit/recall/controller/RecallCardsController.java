package com.bit.recall.controller;

import com.bit.recall.domain.model.CursorResponse;
import com.bit.recall.domain.model.RecallCardResponse;
import com.bit.recall.service.impl.RecallCardsService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

import java.security.Principal;
import java.util.UUID;

@Controller("/recall-cards")
@ExecuteOn(TaskExecutors.IO)
@Secured(SecurityRule.IS_AUTHENTICATED)
@RequiredArgsConstructor
public class RecallCardsController {

    private final RecallCardsService recallCardsService;

    @Get("/{topicId}")
    public CursorResponse<RecallCardResponse> getRecallCardsByTopicId(@PathVariable UUID topicId,
                                                                      @QueryValue(defaultValue = "20") @Min(1) @Max(20) int pageSize,
                                                                      @Nullable @QueryValue("cursor") String cursor,
                                                                      Principal principal) {
        return recallCardsService.fetchRecallCardsByTopicId(
                topicId, UUID.fromString(principal.getName()), pageSize, cursor);
    }

}
