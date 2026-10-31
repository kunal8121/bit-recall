package com.bit.recall.mapper;

import com.bit.recall.domain.RecallItem;
import com.bit.recall.domain.model.RecallCardResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps persisted recall items to the API response, including their JSON metadata. */
@Mapper(componentModel = "jsr330")
public interface RestRecallCardMapper {

    ObjectMapper MAPPER = new ObjectMapper();

    @Mapping(target = "id", expression = "java(recallItem.getId().toString())")
    @Mapping(target = "contentId", expression = "java(recallItem.getContent().getId().toString())")
    @Mapping(target = "topicId", expression = "java(recallItem.getTopic().getId().toString())")
    @Mapping(target = "metaData", expression = "java(toMetaData(recallItem.getMetaData()))")
    RecallCardResponse toRecallCardResponse(RecallItem recallItem);

    default RecallCardResponse.MetaData toMetaData(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, RecallCardResponse.MetaData.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not read recall item metadata", e);
        }
    }
}
