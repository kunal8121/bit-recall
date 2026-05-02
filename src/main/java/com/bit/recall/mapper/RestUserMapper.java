package com.bit.recall.mapper;

import com.bit.recall.domain.RestUser;
import com.bit.recall.domain.RestUserCreateSpec;
import com.bit.recall.domain.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = "jsr330", imports = {UUID.class})
public interface RestUserMapper {
    RestUserMapper INSTANCE = Mappers.getMapper(RestUserMapper.class);

    @Mapping(target = "id", expression = "java(UUID.randomUUID())")
    User toUser(RestUserCreateSpec restUserCreateSpec);

    RestUser toRestUser(User user);
}
