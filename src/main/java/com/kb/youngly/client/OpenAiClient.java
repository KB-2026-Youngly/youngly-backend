package com.kb.youngly.client;

import com.kb.youngly.dto.recommendation.RecommendationPrompt;
import com.kb.youngly.dto.recommendation.RecommendationResponse;

public interface OpenAiClient {

    RecommendationResponse generateRecommendation(RecommendationPrompt prompt);
}
