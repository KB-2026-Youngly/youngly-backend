package com.kb.youngly.dto.recommendation;

public record RecommendationPrompt(
        String systemMessage,
        String userMessage
) {
}
