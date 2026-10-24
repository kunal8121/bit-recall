package com.bit.recall.domain.model;

import io.micronaut.serde.annotation.Serdeable;
import java.util.List;

@Serdeable
public record RecallCardDtoWrapper(List<RecallCardDto> recallCards) {
}
