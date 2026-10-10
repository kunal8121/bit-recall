package com.bit.recall.service;

import com.bit.recall.domain.model.ContentCreatedEvent;

    public interface ContentProcessingService {
        void processContent(ContentCreatedEvent event);
    }
