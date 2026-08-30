package com.bit.recall.mapper;

import com.bit.recall.domain.model.TopicResponse;
import com.bit.recall.domain.Topic;
import com.bit.recall.domain.model.CreateTopicRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

@Mapper(componentModel = "jsr330", imports = {UUID.class}, uses = {RestContentMapper.class})
public interface RestTopicMapper {

    RestTopicMapper INSTANCE = Mappers.getMapper(RestTopicMapper.class);

    @Mapping(target = "id", expression = "java(UUID.randomUUID())")
    @Mapping(target = "user", ignore = true)
    Topic toTopic(CreateTopicRequest spec);

    @JsonIgnoreProperties(ignoreUnknown = true)
    TopicResponse toTopicResponse(Topic topic);
}
