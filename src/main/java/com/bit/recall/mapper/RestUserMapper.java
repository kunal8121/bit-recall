package com.bit.recall.mapper;

import com.bit.recall.domain.model.UserResponse;
import com.bit.recall.domain.model.SignUpRequest;
import com.bit.recall.domain.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

@Mapper(componentModel = "jsr330", imports = {UUID.class})
public interface RestUserMapper {
    RestUserMapper INSTANCE = Mappers.getMapper(RestUserMapper.class);

    @Mapping(target = "id", expression = "java(UUID.randomUUID())")
    @JsonIgnoreProperties(ignoreUnknown = true)
    User toUser(SignUpRequest signUpRequest);

    UserResponse toRestUser(User user);
}
