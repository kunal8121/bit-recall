package com.bit.recall.controller;

import com.bit.recall.domain.RestUser;
import com.bit.recall.domain.RestUserCreateSpec;
import com.bit.recall.domain.User;
import com.bit.recall.mapper.RestUserMapper;
import com.bit.recall.service.UserServiceImpl;
import io.micronaut.http.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@Controller("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserServiceImpl userServiceImpl;

    @Post("/signup")
    public RestUser signUp(@Body @Valid RestUserCreateSpec restUserCreateSpec) {
         User user = userServiceImpl.createUser(RestUserMapper.INSTANCE.toUser(restUserCreateSpec));
         return RestUserMapper.INSTANCE.toRestUser(user);
    }

//    @Get("/{id}")
//    public RestUser get(@PathVariable String id) {
//        User user = userServiceImpl.getUserById(id);
//        return RestUserMapper.INSTANCE.toRestUser(user);
//    }
//
//    @Get
//    public List<RestUser> getAll() {
//        List<User> users = userServiceImpl.findAll();
//        return users.stream().map(RestUserMapper.INSTANCE::toRestUser).toList();
//    }
//
//    @Delete
//    public void delete(@PathVariable String id) {
//        userServiceImpl.deleteUserById(id);
//    }

}

