package com.bit.recall.controller;

import com.bit.recall.domain.model.UserResponse;
import com.bit.recall.domain.model.SignUpRequest;
import com.bit.recall.domain.User;
import com.bit.recall.mapper.RestUserMapper;
import com.bit.recall.security.PasswordEncoder;
import com.bit.recall.service.UserServiceImpl;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@Controller("/users")
@RequiredArgsConstructor
@Secured(SecurityRule.IS_ANONYMOUS) // Allow unauthenticated access for signup
public class UserController {

    private final UserServiceImpl userServiceImpl;
    private final PasswordEncoder passwordEncoder;

    @Post("/signup")
    public UserResponse signUp(@Body @Valid SignUpRequest signUpRequest) {
        User user = RestUserMapper.INSTANCE.toUser(signUpRequest);
        
        // Hash password before saving
        String hashedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(hashedPassword);
        
        User createdUser = userServiceImpl.createUser(user);
        return RestUserMapper.INSTANCE.toRestUser(createdUser);
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

