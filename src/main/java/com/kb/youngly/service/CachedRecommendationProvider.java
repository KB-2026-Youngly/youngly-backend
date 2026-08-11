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
                    "[INFO] 검증된 AI 개인연금 인사이트를 캐시에서 반환합니다. userId={}",
                    userId
            );

            // DB 원본 generationMode(LIVE)는 유지하고, API 응답만 CACHED로 표기한다.
            return RecommendationMapper.toResponse(cached)
                    .withGenerationMode(GenerationMode.CACHED);
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
