package com.kb.youngly.service;

import com.kb.youngly.dto.recommendation.RecommendationResponse;
import com.kb.youngly.enums.GenerationMode;
import com.kb.youngly.enums.GuardrailStatus;
import com.kb.youngly.mapper.RecommendationMapper;
import com.kb.youngly.vo.user.RecommendationVO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class CachedRecommendationProvider implements RecommendationProvider {

    private static final Logger log =
            LogManager.getLogger(CachedRecommendationProvider.class);

    private final RecommendationMapper recommendationMapper;
    private final RecommendationService recommendationService;

    public CachedRecommendationProvider(
            RecommendationMapper recommendationMapper,
            RecommendationService recommendationService
    ) {
        this.recommendationMapper = recommendationMapper;
        this.recommendationService = recommendationService;
    }

    @Override
    public RecommendationResponse provide(String userId) {
        RecommendationVO cached =
                recommendationMapper.selectLatestByUserId(userId);

        if (isUsableLiveRecommendation(cached)) {
            log.info(
                    "[PENSION_INSIGHT_CACHE_HIT] userId={}, recommendationId={}",
                    userId,
                    cached.getRecommendationId()
            );

            // DB 원본 generationMode(LIVE)는 유지하고, API 응답만 CACHED로 표기한다.
            return RecommendationMapper.toResponse(cached)
                    .withGenerationMode(GenerationMode.CACHED);
        }

        log.info("[PENSION_INSIGHT_CACHE_MISS] userId={}", userId);

        return recommendationService.generateRecommendation(userId);
    }

    private boolean isUsableLiveRecommendation(RecommendationVO recommendation) {
        if (recommendation == null) {
            return false;
        }

        boolean liveAndPassed =
                recommendation.getGenerationMode() == GenerationMode.LIVE
                        && recommendation.getGuardrailStatus() == GuardrailStatus.PASSED;

        if (!liveAndPassed) {
            return false;
        }

        return recommendation.getCreatedAt() != null
                && recommendation.getCreatedAt().toLocalDate().isEqual(LocalDate.now());
    }
}
