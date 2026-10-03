package com.bit.recall.service;

import com.bit.recall.domain.Topic;
import com.bit.recall.domain.User;
import com.bit.recall.domain.model.CreateTopicRequest;

import java.util.List;

public interface TopicService {

    Topic createTopicWithUser(CreateTopicRequest request, User user);

    Topic getTopicById(String id, String userId);

    List<Topic> findAll();

    void deleteTopicById(String id, String userId);

    Topic updateTopic(String id, Topic topic, String userId);
}
