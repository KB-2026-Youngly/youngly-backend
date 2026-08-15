package com.kb.youngly.service;

import com.kb.youngly.dto.recommendation.RecommendationResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class RecommendationGenerationAsyncService {

    private final RecommendationService recommendationService;

    public RecommendationGenerationAsyncService(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @Async("recommendationTaskExecutor")
    public void generate(String userId, Long surveyResultId) {
        log.info("[RECOMMENDATION_GENERATION_START] userId={}, surveyResultId={}", userId, surveyResultId);
        try {
            RecommendationResponse response = recommendationService.generateRecommendation(userId, surveyResultId);
            log.info("[RECOMMENDATION_GENERATION_DONE] userId={}, surveyResultId={}, mode={}",
                    userId, surveyResultId, response.generationMode());
        } catch (Exception exception) {
            log.error("[RECOMMENDATION_GENERATION_ERROR] userId={}, surveyResultId={}, errorType={}, reason={}",
                    userId, surveyResultId, exception.getClass().getSimpleName(), exception.getMessage(), exception);
            recommendationService.saveSafeDefaultOnUnexpectedFailure(userId, surveyResultId, exception);
        }
    }
}
