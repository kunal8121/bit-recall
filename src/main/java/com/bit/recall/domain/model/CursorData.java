package com.bit.recall.domain.model;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public record CursorData(Instant lastCreatedAt, UUID entityId) {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static String encodeCursor(CursorData cursorData) {
        try {
            String json = mapper.writeValueAsString(cursorData);

            return Base64.getUrlEncoder()
                         .withoutPadding()
                         .encodeToString(json.getBytes(StandardCharsets.UTF_8));

        } catch (Exception e) {
            throw new IllegalStateException("Could not encode cursor", e);
        }
    }

    public static CursorData decodeCursor(String cursor) {
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(cursor);

            String json = new String(decodedBytes, StandardCharsets.UTF_8);

            return mapper.readValue(json, CursorData.class);

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cursor");
        }
    }
}
