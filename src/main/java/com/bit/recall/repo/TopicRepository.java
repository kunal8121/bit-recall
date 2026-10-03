package com.bit.recall.repo;

import com.bit.recall.domain.Topic;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopicRepository extends JpaRepository<Topic, UUID> {

    Topic findByTitle(String title);

    Optional<Topic> findByIdAndUserId(
            UUID topicId,
            UUID userId
    );
}
