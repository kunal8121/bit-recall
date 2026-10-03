package com.bit.recall.controller;

import com.bit.recall.domain.model.ConfigureAiRequest;
import com.bit.recall.service.AiConfigureService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.security.Principal;

@Controller("/api/{version}/ai/configure")
@Secured(SecurityRule.IS_AUTHENTICATED)
@RequiredArgsConstructor
@ExecuteOn(TaskExecutors.IO)
public class AiConfigureController {

    private static final String VERSION = "1";

    private final AiConfigureService aiConfigureService;

    @Post()
    public HttpResponse<String> configureAi(@Body @Valid ConfigureAiRequest request, Principal principal) {
        String authenticatedUserId = principal.getName();
        aiConfigureService.configureAiProvider(authenticatedUserId, request);
        return HttpResponse.ok();
    }
}
