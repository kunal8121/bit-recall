package com.bit.recall.service.impl;

import com.bit.recall.ai.AiModelFactory;
import com.bit.recall.ai.AiModel;
import com.bit.recall.config.ContentProcessorConfig;
import com.bit.recall.domain.Content;
import com.bit.recall.domain.RevisionDepth;
import com.bit.recall.domain.Topic;
import com.bit.recall.domain.User;
import com.bit.recall.domain.UserAiPreference;
import com.bit.recall.domain.model.ContentCreatedEvent;
import com.bit.recall.domain.model.RecallCardDto;
import com.bit.recall.repo.ContentRepository;
import com.bit.recall.service.AiConfigureService;
import com.bit.recall.service.RecallItemGenerator;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.bit.recall.ai.AIProvider.OPENAI;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ContentProcessingServiceImplTest {

    @Mock AiModelFactory aiModelFactory;
    @Mock RecallItemGenerator recallItemGenerator;
    @Mock ContentRepository contentRepository;
    @Mock EncryptionService encryptionService;
    @Mock AiConfigureService aiConfigureService;
    @Mock RecallCardsService recallCardsService;

    private ContentProcessingServiceImpl createService() {
        return new ContentProcessingServiceImpl(aiModelFactory, recallItemGenerator, contentRepository,
                encryptionService, aiConfigureService, recallCardsService,
                new ContentProcessorConfig(600, 600, 3));
    }

    private ContentCreatedEvent eventFor(UUID contentId, UUID topicId, UUID userId) {
        return ContentCreatedEvent.builder()
                .id(UUID.randomUUID().toString())
                .contentId(contentId.toString())
                .topicId(topicId.toString())
                .userId(userId.toString())
                .build();
    }

    @Test
    void skipsContentThatCannotBeClaimed() {
        UUID contentId = UUID.randomUUID();
        when(contentRepository.claimForProcessing(eq(contentId), any(UUID.class), eq(3), any(Instant.class)))
                .thenReturn(0);
        createService().processContent(eventFor(contentId, UUID.randomUUID(), UUID.randomUUID()));

        verify(contentRepository).claimForProcessing(eq(contentId), any(UUID.class), eq(3), any(Instant.class));
        verify(contentRepository, never()).findById(contentId);
        verifyNoInteractions(aiModelFactory, recallItemGenerator, encryptionService,
                aiConfigureService, recallCardsService);
    }

    @Test
    void generatesAndPersistsRecallCardsForClaimedContent() throws Exception {
        UUID contentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID[] claimedAttemptId = new UUID[1];
        Content content = content(contentId, userId);
        UserAiPreference preference = UserAiPreference.builder()
                .preferredProvider(OPENAI)
                .encryptedApiKey("encrypted-key")
                .build();
        AiModel model = mock(AiModel.class);
        ChatModel chatModel = mock(ChatModel.class);
        List<RecallCardDto> cards = List.of(RecallCardDto.builder()
                .title("Title")
                .recall("Question")
                .explanation("Answer")
                .build());
        when(contentRepository.claimForProcessing(eq(contentId), any(UUID.class), eq(3), any(Instant.class)))
                .thenAnswer(invocation -> {
                    claimedAttemptId[0] = invocation.getArgument(1);
                    return 1;
                });
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(aiConfigureService.getPreferenceForUser(userId)).thenReturn(preference);
        when(aiModelFactory.getModel(OPENAI)).thenReturn(model);
        when(encryptionService.decrypt("encrypted-key")).thenReturn("api-key");
        when(model.getChatModel("api-key")).thenReturn(chatModel);
        when(recallItemGenerator.generateRecallItems(eq(chatModel), anyString(), contains("SOURCE MATERIAL:")))
                .thenReturn(cards);

        createService().processContent(eventFor(contentId, content.getTopic().getId(), userId));

        verify(recallItemGenerator).generateRecallItems(eq(chatModel), anyString(),
                argThat(prompt -> prompt.contains("Revision Depth: BALANCED") && prompt.contains("source text")));
        verify(recallCardsService).persist(cards, content.getTopic().getId(), contentId, claimedAttemptId[0]);
        verify(contentRepository, never()).markAttemptFailed(eq(contentId), any(UUID.class), any(Instant.class));
    }

    @Test
    void marksAttemptFailedAndWrapsGenerationErrors() {
        UUID contentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Content content = content(contentId, userId);
        UserAiPreference preference = UserAiPreference.builder()
                .preferredProvider(OPENAI)
                .encryptedApiKey("encrypted-key")
                .build();
        AiModel model = mock(AiModel.class);
        ChatModel chatModel = mock(ChatModel.class);
        RuntimeException generationFailure = new RuntimeException("AI unavailable");
        when(contentRepository.claimForProcessing(eq(contentId), any(UUID.class), eq(3), any(Instant.class)))
                .thenReturn(1);
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(aiConfigureService.getPreferenceForUser(userId)).thenReturn(preference);
        when(aiModelFactory.getModel(OPENAI)).thenReturn(model);
        when(encryptionService.decrypt("encrypted-key")).thenReturn("api-key");
        when(model.getChatModel("api-key")).thenReturn(chatModel);
        when(recallItemGenerator.generateRecallItems(eq(chatModel), anyString(), anyString()))
                .thenThrow(generationFailure);

        assertThrows(com.bit.recall.exception.BitRecallException.class,
                () -> createService().processContent(eventFor(contentId, content.getTopic().getId(), userId)));

        verify(contentRepository).markAttemptFailed(eq(contentId), any(UUID.class), any(Instant.class));
        verifyNoInteractions(recallCardsService);
    }

    private Content content(UUID contentId, UUID userId) {
        User user = User.builder().id(userId).build();
        Topic topic = Topic.builder().id(UUID.randomUUID()).user(user).build();
        return Content.builder()
                .id(contentId)
                .topic(topic)
                .text("source text")
                .revisionDepth(RevisionDepth.BALANCED)
                .build();
    }
}
