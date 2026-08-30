package com.bit.recall.mapper;

import com.bit.recall.domain.Content;
import com.bit.recall.domain.model.CreateContentRequest;
import com.bit.recall.domain.model.ContentResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

@Mapper(componentModel = "jsr330", imports = {UUID.class})
public interface RestContentMapper {

    RestContentMapper INSTANCE = Mappers.getMapper(RestContentMapper.class);

    @Mapping(target = "id", expression = "java(UUID.randomUUID())")
    @Mapping(target = "createdAt", ignore = true)
    Content toContent(CreateContentRequest createContentRequest);

    @JsonIgnoreProperties(ignoreUnknown = true)
    ContentResponse toContentResponse(Content content);

}
