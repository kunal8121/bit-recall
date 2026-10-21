package com.bit.recall.ai;

import com.bit.recall.domain.model.RecallCardDto;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.micronaut.langchain4j.annotation.AiService;

import java.util.List;

/** Typed LangChain4j AI Service used by {@link OpenAIRecallItemGenerator}. */
public interface RecallItemExtractor {

    @SystemMessage("{{systemPrompt}}")
    @UserMessage("{{userPrompt}}")
    List<RecallCardDto> extractRecallItems(@V("systemPrompt") String systemPrompt,
                                           @V("userPrompt") String userPrompt);
}
