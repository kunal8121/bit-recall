package com.bit.recall.controller;

import com.bit.recall.domain.model.CreateTopicRequest;
import com.bit.recall.domain.model.TopicResponse;
import com.bit.recall.domain.Topic;
import com.bit.recall.domain.User;
import com.bit.recall.mapper.RestTopicMapper;
import com.bit.recall.repo.UserRepository;
import com.bit.recall.service.TopicService;
import com.bit.recall.service.impl.TopicServiceImpl;
import io.micronaut.core.version.annotation.Version;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.security.Principal;
import java.util.List;
import java.util.NoSuchElementException;

@Controller("api/{version}/topics")
@RequiredArgsConstructor
@Secured(SecurityRule.IS_AUTHENTICATED)
@ExecuteOn(TaskExecutors.IO)
public class TopicController {
    private static final String VERSION = "1";
    private final TopicService topicsService;
    private final UserRepository userRepository;

    @Version(VERSION)
    @Post
    public TopicResponse createForAuthenticatedUser(
            @Body @Valid CreateTopicRequest request,
            Principal principal) {

        String username = principal.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found with id: " + username));

        Topic createdTopic = topicsService.createTopicWithUser(request, user);
        return RestTopicMapper.INSTANCE.toTopicResponse(createdTopic);
    }

    @Version(VERSION)
    @Get("/{id}")
    public TopicResponse get(@PathVariable String id, Principal principal) {
        Topic Topic = topicsService.getTopicById(id, principal.getName());
        return RestTopicMapper.INSTANCE.toTopicResponse(Topic);
    }

    @Version(VERSION)
    @Put("/{id}")
    public TopicResponse update(@PathVariable String id,
                                @Body @Valid CreateTopicRequest restCreateTopicRequest,
                                Principal principal) {
        Topic Topic = topicsService.updateTopic(id, RestTopicMapper.INSTANCE.toTopic(restCreateTopicRequest), principal.getName());
        return RestTopicMapper.INSTANCE.toTopicResponse(Topic);
    }

    @Version(VERSION)
    @Get
    public List<TopicResponse> getAll(Principal principal) {
        List<Topic> Topics = topicsService.findAll(principal.getName());
        return Topics.stream().map(RestTopicMapper.INSTANCE::toTopicResponse).toList();
    }

    @Version(VERSION)
    @Delete("/{id}")
    public void delete(@PathVariable String id, Principal principal) {
        topicsService.deleteTopicById(id, principal.getName());
    }
}
