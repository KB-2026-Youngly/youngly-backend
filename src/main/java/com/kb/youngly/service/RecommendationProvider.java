package com.kb.youngly.service;

import com.kb.youngly.dto.recommendation.RecommendationResponse;

public interface RecommendationProvider {
    RecommendationResponse provide(String userId);
}
