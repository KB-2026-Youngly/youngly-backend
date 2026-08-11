package com.kb.youngly.controller;

import com.kb.youngly.dto.recommendation.YounglyRecommendationResponse;
import com.kb.youngly.service.RecommendationProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pension/insight")
public class PensionInsightController {
    private final RecommendationProvider recommendationProvider;

    public PensionInsightController(RecommendationProvider recommendationProvider) {
        this.recommendationProvider = recommendationProvider;
    }

    @GetMapping("/{userId}")
    public YounglyRecommendationResponse getInsight(@PathVariable String userId) {
        return recommendationProvider.provide(userId);
    }
}
