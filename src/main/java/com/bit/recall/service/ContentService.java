package com.bit.recall.service;

import com.bit.recall.domain.Content;
import com.bit.recall.domain.model.ContentResponse;
import com.bit.recall.domain.model.CreateContentRequest;

import java.util.List;
import java.util.Optional;

public interface ContentService {
    Content createContent(String topicId, CreateContentRequest createContentRequest, String userId);

    Optional<Content> findById(String contentId, String userId);

    List<ContentResponse> findAllByTopicId(String topicId, String userId);

    void deleteContentById(String contentId, String userId);

    Content updateContent(String contentId, Content Content, String userId);
}
