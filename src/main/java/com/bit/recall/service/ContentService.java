package com.bit.recall.service;

import com.bit.recall.domain.Content;
import com.bit.recall.domain.model.ContentResponse;
import com.bit.recall.domain.model.CreateContentRequest;

import java.util.List;
import java.util.Optional;

public interface ContentService {
    Content createContent(String topicId, CreateContentRequest createContentRequest);

    Optional<Content> findById(String contentId);

    List<ContentResponse> findAllByTopicId(String topicId);

    void deleteContentById(String contentId);

    Content updateContent(String contentId, Content Content);
}
