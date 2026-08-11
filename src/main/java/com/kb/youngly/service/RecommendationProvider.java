package com.kb.youngly.service;

import com.kb.youngly.dto.recommendation.YounglyRecommendationResponse;

public interface RecommendationProvider {
    YounglyRecommendationResponse provide(String userId);
}
