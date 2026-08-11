package com.kb.youngly.client;

import com.kb.youngly.dto.recommendation.RecommendationPrompt;
import com.kb.youngly.dto.recommendation.YounglyRecommendationResponse;

public interface OpenAiClient {

    YounglyRecommendationResponse generateRecommendation(RecommendationPrompt prompt);
}
