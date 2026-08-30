package com.bit.recall.controller;

import com.bit.recall.domain.model.CreateContentRequest;
import com.bit.recall.domain.model.ContentResponse;
import com.bit.recall.mapper.RestContentMapper;
import com.bit.recall.service.ContentService;
import io.micronaut.core.version.annotation.Version;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Controller("api/{version}")
@Secured(SecurityRule.IS_AUTHENTICATED)
@RequiredArgsConstructor
public class ContentController {

    private static final String VERSION = "1";
    private final ContentService contentService;

    @Version(VERSION)
    @Post("/topics/{topicId}/contents")
    public ContentResponse createContent(@PathVariable String topicId, @Body @Valid CreateContentRequest createContentRequest) {
         var content =  contentService.createContent(topicId, createContentRequest);
         return RestContentMapper.INSTANCE.toContentResponse(content);
    }

    @Version(VERSION)
    @Get("/{id}")
    public ContentResponse getContentById(@PathVariable String id) {
        var content = contentService.findById(id);
        return RestContentMapper.INSTANCE.toContentResponse(content.get());
    }

    @Version(VERSION)
    @Get("/topics/{topicId}/contents")
    public List<ContentResponse> getContentsByTopicId(@PathVariable String topicId) {
        return contentService.findAllByTopicId(topicId);
    }

    @Version(VERSION)
    @Put("contents/{contentId}")
    public ContentResponse updateContent( @PathVariable String contentId, @Body @Valid CreateContentRequest createContentRequest) {
        var content = contentService.updateContent(contentId, RestContentMapper.INSTANCE.toContent(createContentRequest));
        return RestContentMapper.INSTANCE.toContentResponse(content);
    }

    @Version(VERSION)
    @Delete("contents/{contentId}")
    public void deleteContent(@PathVariable String contentId) {
         contentService.deleteContentById(contentId);
     }
}
