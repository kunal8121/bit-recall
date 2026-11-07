package com.bit.recall.service.impl;

import com.bit.recall.ai.AIProvider;
import com.bit.recall.ai.AiModel;
import com.bit.recall.ai.AiModelFactory;
import com.bit.recall.exception.BitRecallException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiModelFactoryTest {
    @Mock AiModel model;

    @Test
    void returnsAdapterForConfiguredProvider() {
        when(model.getProvider()).thenReturn(AIProvider.OPENAI);
        AiModelFactory factory = new AiModelFactory(List.of(model));

        assertSame(model, factory.getModel(AIProvider.OPENAI));
    }

    @Test
    void rejectsUnconfiguredProvider() {
        AiModelFactory factory = new AiModelFactory(List.of());

        assertThrows(BitRecallException.class, () -> factory.getModel(AIProvider.OPENAI));
    }
}
