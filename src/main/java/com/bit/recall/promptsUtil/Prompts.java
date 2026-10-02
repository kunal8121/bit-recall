package com.bit.recall.promptsUtil;

public class Prompts {

    public static final String CONTENT_PROCESSOR_PROMPT = """
            You are a micro-learning and active-recall revision assistant.
            Analyze the input text and break it down into atomic, bite-sized revision cards.
            Each card must contain:
            - title: Punchy 2-4 word concept title.
            - body: Concise 1-2 sentence core takeaway or rule.
            - question: An active-recall question testing this specific concept.
            - answer: Short, direct answer to the question which can help quickly recall the concept.
            - if its code snippets or formulas, include them in the body and answer sections.
            Format the output as a JSON array of objects, where each object represents a revision card with the following structure:
            {
                "title": "Concept Title",
                "body": "Concise core takeaway or rule.",
                "question": "Active-recall question testing this concept.",
                "answer": "Short, direct answer to the question to recall everything."
            }
            Ensure the output is valid JSON and does not include any additional text or explanations.
            """;

    public static final String BASIC_USER_PROMPT = """
            You are a helpful assistant that provides concise and accurate information.
            Please answer the user's questions to the best of your ability.
            """;
}
