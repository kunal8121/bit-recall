package com.bit.recall.controller;

import com.bit.recall.domain.model.CreateContentRequest;
import com.bit.recall.domain.model.ContentResponse;
import com.bit.recall.mapper.RestContentMapper;
import com.bit.recall.service.ContentService;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
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

@Controller("api/{version}")
@Secured(SecurityRule.IS_AUTHENTICATED)
@RequiredArgsConstructor
@ExecuteOn(TaskExecutors.IO)
public class ContentController {

    private static final String VERSION = "1";
    private final ContentService contentService;
//    private final RateLimiterRegistry rateLimiterRegistry;

    @Version(VERSION)
    @Post("/topics/{topicId}/contents")
    public ContentResponse createContent(@PathVariable String topicId,
                                         @Body @Valid CreateContentRequest createContentRequest,
                                         Principal principal) {
//        var userId = principal.getName();
//        var limiter = rateLimiterRegistry.rateLimiter("tenant-" + userId, "aiServiceRateLimiter");
//        if (!limiter.acquirePermission()) {
//            throw new RuntimeException("Rate limit exceeded for user: " + userId);
//        }
        var content =  contentService.createContent(topicId, createContentRequest, principal.getName());
        return RestContentMapper.INSTANCE.toContentResponse(content);
    }

    @Version(VERSION)
    @Get("/{id}")
    public ContentResponse getContentById(@PathVariable String id,
                                          Principal principal) {
        var content = contentService.findById(id, principal.getName());
        return RestContentMapper.INSTANCE.toContentResponse(content.get());
    }

    @Version(VERSION)
    @Get("/topics/{topicId}/contents")
    public List<ContentResponse> getContentsByTopicId(@PathVariable String topicId,
                                                      Principal principal) {
        return contentService.findAllByTopicId(topicId, principal.getName());
    }

    @Version(VERSION)
    @Put("contents/{contentId}")
    public ContentResponse updateContent( @PathVariable String contentId,
                                          @Body @Valid CreateContentRequest createContentRequest,
                                          Principal principal) {
        var content = contentService.updateContent(contentId,
                                                   RestContentMapper.INSTANCE.toContent(createContentRequest),
                                                   principal.getName());
        return RestContentMapper.INSTANCE.toContentResponse(content);
    }

    @Version(VERSION)
    @Delete("contents/{contentId}")
    public void deleteContent(@PathVariable String contentId,
                              Principal principal) {
         contentService.deleteContentById(contentId, principal.getName());
     }

    @Version(VERSION)
    @Post("contents/{contentId}/retry")
    public ContentResponse retryContent(@PathVariable String contentId, Principal principal) {
        return RestContentMapper.INSTANCE.toContentResponse(contentService.retryContent(contentId, principal.getName()));
    }
}
