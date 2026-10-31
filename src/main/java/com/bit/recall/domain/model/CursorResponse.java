package com.bit.recall.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;

import java.util.List;

@Serdeable
@Builder
public record CursorResponse<T>(@JsonProperty("page_size") Integer pageSize,
                                @JsonProperty("has_next") Boolean hasNext,
                                @JsonProperty("next_cursor") String nextCursor,
                                @JsonProperty("data") List<T> data) {

}
