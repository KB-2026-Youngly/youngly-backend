package com.kb.youngly.service;

import com.kb.youngly.dto.recommendation.YounglyRecommendationResponse;
import com.kb.youngly.mapper.RecommendationMapper;
import com.kb.youngly.vo.user.RecommendationVO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

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
    public YounglyRecommendationResponse provide(String userId) {
        RecommendationVO cached =
                recommendationMapper.selectLatestByUserId(userId);

        if (isUsableLiveRecommendation(cached)) {
            log.info(
                    "[INFO] 검증된 AI 개인연금 인사이트를 캐시에서 반환합니다. userId={}",
                    userId
            );

            return RecommendationMapper.toResponse(cached);
        }

        if (cached != null) {
            log.info(
                    "[INFO] 기존 추천 결과가 LIVE/PASSED가 아니므로 새로 생성합니다. "
                            + "userId={}, generationMode={}, guardrailStatus={}",
                    userId,
                    cached.getGenerationMode(),
                    cached.getGuardrailStatus()
            );
        } else {
            log.info(
                    "[INFO] 저장된 인사이트가 없어 새로 생성합니다. userId={}",
                    userId
            );
        }

        return recommendationService.generateRecommendation(userId);
    }

    private boolean isUsableLiveRecommendation(RecommendationVO recommendation) {
        if (recommendation == null) return false;
        boolean liveAndPassed = "LIVE".equals(String.valueOf(recommendation.getGenerationMode()))
                && "PASSED".equals(String.valueOf(recommendation.getGuardrailStatus()));
        if (!liveAndPassed) return false;

        return recommendation.getCreatedAt() != null
                && recommendation.getCreatedAt().toLocalDate().isEqual(java.time.LocalDate.now());
    }
}