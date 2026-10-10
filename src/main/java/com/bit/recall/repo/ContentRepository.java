package com.bit.recall.repo;

import com.bit.recall.domain.Content;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContentRepository extends JpaRepository<Content, UUID> {

    List<Content> findByTopicId(UUID topicId);

    List<Content> findByStatus(Content.Status status);
}
