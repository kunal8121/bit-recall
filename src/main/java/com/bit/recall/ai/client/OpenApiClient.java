package com.bit.recall.ai.client;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;

import io.micronaut.http.client.annotation.Client;
import io.micronaut.retry.annotation.CircuitBreaker;
import jakarta.validation.Valid;

@Client("openai")
@CircuitBreaker(
    attempts = "3",
    delay = "2s",
    multiplier = "2",
    reset = "30s"
)
public interface OpenApiClient {

    @Post("/chat/completions")
    OpenApiResponse generateCompletion(@Header("Authorization") String apiKey, @Body @Valid OpenApiRequest requestBody);
}
