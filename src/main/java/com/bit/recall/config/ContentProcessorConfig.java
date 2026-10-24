package com.bit.recall.config;

import io.micronaut.context.annotation.ConfigurationProperties;

@ConfigurationProperties("bit-recall.processing")
public record ContentProcessorConfig(Integer staleAfter, Integer orphanAfter, Integer maxRetries) {
}
