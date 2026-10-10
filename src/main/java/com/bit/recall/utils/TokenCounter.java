package com.bit.recall.utils;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import jakarta.inject.Singleton;

@Singleton
public class TokenCounter {

    private final Encoding encoding;

    public TokenCounter() {
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        this.encoding = registry.getEncoding(EncodingType.O200K_BASE);
    }

    public int countTokens(String text) {
        return encoding.countTokens(text);
    }
}
