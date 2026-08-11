package com.kb.youngly.service;

import com.kb.youngly.client.OpenAiClient;
import com.kb.youngly.dto.recommendation.GuardrailResult;
import com.kb.youngly.dto.recommendation.PensionForecastFacts;
import com.kb.youngly.dto.recommendation.RecommendationPrompt;
import com.kb.youngly.dto.recommendation.RecommendationPromptContext;
import com.kb.youngly.dto.recommendation.YounglyRecommendationResponse;
import com.kb.youngly.enums.GenerationMode;
import com.kb.youngly.enums.GuardrailStatus;
import com.kb.youngly.enums.Baseline;
import com.kb.youngly.mapper.InterestMapper;
import com.kb.youngly.mapper.MarketDailySnapshotMapper;
import com.kb.youngly.mapper.RecommendationMapper;
import com.kb.youngly.mapper.SurveyMapper;
import com.kb.youngly.mapper.UserMapper;
import com.kb.youngly.prompt.RecommendationPromptBuilder;
import com.kb.youngly.vo.market.MarketDailySnapshotVO;
import com.kb.youngly.vo.survey.InterestVO;
import com.kb.youngly.vo.survey.SurveyResultVO;
import com.kb.youngly.vo.user.UserVO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
public class RecommendationService {

    private static final Logger log = LogManager.getLogger(RecommendationService.class);

    private static final int MARKET_CONTEXT_MAX_LENGTH = 4_000;
    private static final int RAW_TEXT_PER_DAY_MAX_LENGTH = 500;
    private static final int MAX_LLM_ATTEMPTS = 1;

    private final UserMapper userMapper;
    private final SurveyMapper surveyMapper;
    private final InterestMapper interestMapper;
    private final MarketDailySnapshotMapper marketDailySnapshotMapper;
    private final RecommendationMapper recommendationMapper;
    private final RecommendationPromptBuilder promptBuilder;
    private final ObjectProvider<OpenAiClient> openAiClientProvider;
    private final MarketSnapshotIngestService marketSnapshotIngestService;
    private final PensionForecastService pensionForecastService;
    private final NumericGuardrailValidator guardrailValidator;
    private final Environment environment;

    public RecommendationService(
            UserMapper userMapper,
            SurveyMapper surveyMapper,
            InterestMapper interestMapper,
            MarketDailySnapshotMapper marketDailySnapshotMapper,
            RecommendationMapper recommendationMapper,
            RecommendationPromptBuilder promptBuilder,
            ObjectProvider<OpenAiClient> openAiClientProvider,
            MarketSnapshotIngestService marketSnapshotIngestService,
            PensionForecastService pensionForecastService,
            NumericGuardrailValidator guardrailValidator,
            Environment environment
    ) {
        this.userMapper = userMapper;
        this.surveyMapper = surveyMapper;
        this.interestMapper = interestMapper;
        this.marketDailySnapshotMapper = marketDailySnapshotMapper;
        this.recommendationMapper = recommendationMapper;
        this.promptBuilder = promptBuilder;
        this.openAiClientProvider = openAiClientProvider;
        this.marketSnapshotIngestService = marketSnapshotIngestService;
        this.pensionForecastService = pensionForecastService;
        this.guardrailValidator = guardrailValidator;
        this.environment = environment;
    }

    public YounglyRecommendationResponse generateRecommendation(String userId) {
        UserVO user = userMapper.findByUserId(userId);

        if (user == null) {
            throw new IllegalArgumentException(
                    "사용자를 찾을 수 없습니다. userId=" + userId
            );
        }

        SurveyResultVO surveyResult =
                surveyMapper.selectLatestResultByUserId(userId);

        if (surveyResult == null || !StringUtils.hasText(surveyResult.getBaseline())) {
            throw new IllegalStateException(
                    "사용자의 최신 투자성향 설문 결과가 없습니다. userId=" + userId
            );
        }

        PensionForecastFacts facts =
                pensionForecastService.calculateForecast(userId);

        String ageBand = resolveAgeBand(user.getBirthday());
        String baseline = surveyResult.getBaseline();
        String baselineLabel = resolveBaselineLabel(baseline);

        List<String> investmentInterests =
                findInterestNames(userId, true);

        List<String> hobbyInterests =
                findInterestNames(userId, false);

        String marketContext =
                buildRecentMarketContextWithFallback();

        RecommendationPromptContext context =
                new RecommendationPromptContext(
                        userId,
                        ageBand,
                        baseline,
                        baselineLabel,
                        investmentInterests,
                        hobbyInterests,
                        marketContext,
                        facts
                );

        RecommendationPrompt prompt = promptBuilder.build(context);

        YounglyRecommendationResponse response = null;
        GuardrailResult lastResult = null;

        for (int attempt = 1; attempt <= MAX_LLM_ATTEMPTS; attempt++) {
            try {
                YounglyRecommendationResponse candidate =
                        openAiClientProvider.getObject()
                                .generateRecommendation(prompt);

                lastResult = guardrailValidator.validate(candidate, facts);

                if (lastResult.passed()) {
                    response = candidate.withFacts(
                            facts,
                            GenerationMode.LIVE,
                            GuardrailStatus.PASSED
                    );
                    break;
                }

                log.warn(
                        "[WARN] 가드레일 검증 실패 (attempt={}): {}",
                        attempt,
                        lastResult.reason()
                );

            } catch (Exception e) {
                lastResult = GuardrailResult.failed(e.getMessage());

                log.warn(
                        "[WARN] 추천 LLM 호출 실패 (attempt={}). userId={}",
                        attempt,
                        userId,
                        e
                );
            }
        }

        if (response == null) {
            log.warn(
                    "[WARN] {}회 재시도 후에도 가드레일 통과 실패. "
                            + "텍스트 필드는 비우고 facts만 반환합니다. "
                            + "userId={}, reason={}",
                    MAX_LLM_ATTEMPTS,
                    userId,
                    lastResult == null ? null : lastResult.reason()
            );

            response = createSafeDefaultReport()
                    .withFacts(
                            facts,
                            GenerationMode.SAFE_DEFAULT,
                            GuardrailStatus.FAILED_FALLBACK
                    );
        }

        recommendationMapper.insert(
                RecommendationMapper.toVO(userId, baseline, response)
        );

        return response;
    }

    private String resolveAgeBand(LocalDate birthday) {
        if (birthday == null) {
            return "미상";
        }

        int age = Period.between(birthday, LocalDate.now()).getYears();
        int ageBand = Math.max(age / 10 * 10, 0);

        return ageBand + "대";
    }

    private String resolveBaselineLabel(String baselineValue) {
        try {
            return Baseline.valueOf(baselineValue).getLabel();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "알 수 없는 투자성향 값입니다. baseline=" + baselineValue,
                    e
            );
        }
    }

    private List<String> findInterestNames(
            String userId,
            boolean investment
    ) {
        return interestMapper.findUserInterestsByInvestment(userId, investment)
                .stream()
                .map(InterestVO::getInterestName)
                .filter(StringUtils::hasText)
                .toList();
    }

    private String buildRecentMarketContextWithFallback() {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(14);

        List<MarketDailySnapshotVO> snapshots =
                marketDailySnapshotMapper.findRecentSnapshots(start, end, 20);

        if (snapshots.isEmpty() && isDevLikeProfile()) {
            try {
                marketSnapshotIngestService.ingestRecentTwoWeeks();

                snapshots = marketDailySnapshotMapper
                        .findRecentSnapshots(start, end, 20);

            } catch (Exception e) {
                log.warn(
                        "[WARN] 시장동향 fallback ingest 실패. "
                                + "빈 시장 컨텍스트로 계속 진행합니다.",
                        e
                );
            }
        }

        StringBuilder builder = new StringBuilder();

        for (MarketDailySnapshotVO snapshot : snapshots) {
            String text = StringUtils.hasText(snapshot.getSummaryText())
                    ? snapshot.getSummaryText()
                    : abbreviate(
                    snapshot.getRawText(),
                    RAW_TEXT_PER_DAY_MAX_LENGTH
            );

            if (!StringUtils.hasText(text)) {
                continue;
            }

            appendWithinLimit(
                    builder,
                    "[" + snapshot.getMarketDate() + "] " + text.trim()
            );

            if (builder.length() >= MARKET_CONTEXT_MAX_LENGTH) {
                break;
            }
        }

        return builder.toString();
    }

    private void appendWithinLimit(
            StringBuilder builder,
            String text
    ) {
        if (builder.length() > 0) {
            text = "\n\n" + text;
        }

        int remaining = MARKET_CONTEXT_MAX_LENGTH - builder.length();

        if (remaining > 0) {
            builder.append(text, 0, Math.min(text.length(), remaining));
        }
    }

    private String abbreviate(
            String text,
            int maxLength
    ) {
        if (!StringUtils.hasText(text)) {
            return "";
        }

        String compact = text.replaceAll("\\s+", " ").trim();

        return compact.length() <= maxLength
                ? compact
                : compact.substring(0, maxLength);
    }

    private boolean isDevLikeProfile() {
        for (String profile : environment.getActiveProfiles()) {
            if ("dev".equalsIgnoreCase(profile)
                    || "development".equalsIgnoreCase(profile)
                    || "demo".equalsIgnoreCase(profile)) {
                return true;
            }
        }

        return false;
    }

    private YounglyRecommendationResponse createSafeDefaultReport() {
        return YounglyRecommendationResponse.llmTextOnly(
                List.of(),
                null,
                null,
                null
        );
    }
}