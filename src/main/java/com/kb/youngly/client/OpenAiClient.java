package com.kb.youngly.client;

import com.kb.youngly.dto.recommendation.RecommendationPrompt;
import com.kb.youngly.dto.recommendation.RecommendationResponse;
import com.kb.youngly.dto.market.MarketSummaryResponse;

public interface OpenAiClient {

    RecommendationResponse generateRecommendation(RecommendationPrompt prompt);

    MarketSummaryResponse generateMarketSummary(RecommendationPrompt prompt);
}
