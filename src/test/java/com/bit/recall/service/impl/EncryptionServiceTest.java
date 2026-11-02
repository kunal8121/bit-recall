package com.bit.recall.service.impl;

import com.bit.recall.exception.BitRecallException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EncryptionServiceTest {
    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void roundTripsAndUsesFreshIv() {
        EncryptionService service = new EncryptionService(KEY);
        String first = service.encrypt("secret");
        String second = service.encrypt("secret");
        assertEquals("secret", service.decrypt(first));
        assertNotEquals(first, second);
    }
    @Test
    void rejectsTamperedCiphertext() {
        EncryptionService service = new EncryptionService(KEY);
        byte[] data = Base64.getDecoder().decode(service.encrypt("secret"));
        data[data.length - 1] ^= 1;
        assertThrows(BitRecallException.class, () -> service.decrypt(Base64.getEncoder().encodeToString(data)));
    }
}
