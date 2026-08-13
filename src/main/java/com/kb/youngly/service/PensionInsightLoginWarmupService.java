package com.kb.youngly.service;

import com.kb.youngly.enums.GenerationMode;
import com.kb.youngly.enums.GuardrailStatus;
import com.kb.youngly.mapper.RecommendationMapper;
import com.kb.youngly.vo.user.RecommendationVO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 로그인 성공 후 개인연금 인사이트를 비동기로 미리 생성한다.
 * 당일 LIVE+PASSED 캐시가 있으면 OpenAI를 호출하지 않는다.
 */
@Service
public class PensionInsightLoginWarmupService {

    private static final Logger log =
            LogManager.getLogger(PensionInsightLoginWarmupService.class);

    private final Set<String> inProgressUserIds = ConcurrentHashMap.newKeySet();

    private final RecommendationMapper recommendationMapper;
    private final RecommendationProvider recommendationProvider;

    public PensionInsightLoginWarmupService(
            RecommendationMapper recommendationMapper,
            RecommendationProvider recommendationProvider
    ) {
        this.recommendationMapper = recommendationMapper;
        this.recommendationProvider = recommendationProvider;
    }

    /**
     * 로그인 응답을 지연시키지 않도록 별도 스레드에서 실행한다.
     * 예외는 로그로만 남기고 호출자에게 전파하지 않는다.
     */
    @Async
    public void warmUpAfterLogin(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }

        if (hasUsableSameDayLivePassed(userId)) {
            log.info("[PENSION_INSIGHT_LOGIN_WARMUP_SKIPPED] userId={}", userId);
            return;
        }

        if (!inProgressUserIds.add(userId)) {
            log.info("[PENSION_INSIGHT_LOGIN_WARMUP_SKIPPED] userId={}", userId);
            return;
        }

        try {
            log.info("[PENSION_INSIGHT_LOGIN_WARMUP_START] userId={}", userId);
            recommendationProvider.provide(userId);
        } catch (Exception e) {
            log.warn(
                    "[PENSION_INSIGHT_LOGIN_WARMUP_FAILED] userId={}",
                    userId,
                    e
            );
        } finally {
            inProgressUserIds.remove(userId);
        }
    }

    private boolean hasUsableSameDayLivePassed(String userId) {
        RecommendationVO latest = recommendationMapper.selectLatestByUserId(userId);
        if (latest == null) {
            return false;
        }

        boolean liveAndPassed =
                latest.getGenerationMode() == GenerationMode.LIVE
                        && latest.getGuardrailStatus() == GuardrailStatus.PASSED;

        if (!liveAndPassed) {
            return false;
        }

        return latest.getCreatedAt() != null
                && latest.getCreatedAt().toLocalDate().isEqual(LocalDate.now());
    }
}
