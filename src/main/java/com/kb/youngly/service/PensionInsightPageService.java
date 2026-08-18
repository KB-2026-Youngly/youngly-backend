package com.kb.youngly.service;

import com.kb.youngly.dto.recommendation.PensionForecastFacts;
import com.kb.youngly.dto.recommendation.PensionInsightPageResponse;
import com.kb.youngly.enums.GenerationMode;
import com.kb.youngly.enums.GuardrailStatus;
import com.kb.youngly.enums.PersonalInsightStatus;
import com.kb.youngly.mapper.InterestMapper;
import com.kb.youngly.mapper.MarketDailySnapshotMapper;
import com.kb.youngly.mapper.RecommendationMapper;
import com.kb.youngly.mapper.SurveyMapper;
import com.kb.youngly.mapper.UserMapper;
import com.kb.youngly.util.ModelOutputArtifactValidator;
import com.kb.youngly.vo.market.MarketDailySnapshotVO;
import com.kb.youngly.vo.survey.InterestVO;
import com.kb.youngly.vo.survey.SurveyResultVO;
import com.kb.youngly.vo.user.RecommendationVO;
import com.kb.youngly.vo.user.UserVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class PensionInsightPageService {

    private static final String NO_INVESTMENT_INTEREST_LABEL = "해당 없음";
    private static final String DEFAULT_MARKET_HEADLINE = "최근 시장동향 요약 준비 중";
    private static final String DEFAULT_MARKET_DETAIL = "시장동향 원문 수집 또는 공용 요약 생성이 완료되면 이 영역에 자세한 설명이 표시됩니다.";

    private final UserMapper userMapper;
    private final SurveyMapper surveyMapper;
    private final InterestMapper interestMapper;
    private final MarketDailySnapshotMapper marketDailySnapshotMapper;
    private final RecommendationMapper recommendationMapper;
    private final PensionForecastService pensionForecastService;

    public PensionInsightPageService(UserMapper userMapper,
                                     SurveyMapper surveyMapper,
                                     InterestMapper interestMapper,
                                     MarketDailySnapshotMapper marketDailySnapshotMapper,
                                     RecommendationMapper recommendationMapper,
                                     PensionForecastService pensionForecastService) {
        this.userMapper = userMapper;
        this.surveyMapper = surveyMapper;
        this.interestMapper = interestMapper;
        this.marketDailySnapshotMapper = marketDailySnapshotMapper;
        this.recommendationMapper = recommendationMapper;
        this.pensionForecastService = pensionForecastService;
    }

    @Transactional(readOnly = true)
    public PensionInsightPageResponse getInsightPage(String userId) {
        UserVO user = userMapper.findByUserId(userId);
        if (user == null) {
            throw new NoSuchElementException("사용자를 찾을 수 없습니다. userId=" + userId);
        }

        PensionForecastFacts facts = pensionForecastService.calculateForecast(userId);
        PensionInsightPageResponse.Forecast forecast = toForecast(facts);
        PensionInsightPageResponse.MarketSummary marketSummary = findMarketSummary();
        PensionInsightPageResponse.UserInterests userInterests = findUserInterests(userId);

        SurveyResultVO surveyResult = surveyMapper.selectLatestResultByUserId(userId);
        if (surveyResult == null || !StringUtils.hasText(surveyResult.getBaseline())) {
            return new PensionInsightPageResponse(
                    forecast,
                    marketSummary,
                    userInterests,
                    PersonalInsightStatus.SURVEY_REQUIRED,
                    null
            );
        }

        RecommendationVO latest = recommendationMapper.selectLatestByUserId(userId);
        if (isSameDayLivePassed(latest, surveyResult.getSurveyResultId())) {
            return new PensionInsightPageResponse(
                    forecast,
                    marketSummary,
                    userInterests,
                    PersonalInsightStatus.READY,
                    new PensionInsightPageResponse.PersonalInsight(
                            latest.getPensionInsightIntro(),
                            latest.getPensionInsightStrategy()
                    )
            );
        }

        if (isSameDaySafeDefault(latest, surveyResult.getSurveyResultId())) {
            return new PensionInsightPageResponse(
                    forecast,
                    marketSummary,
                    userInterests,
                    PersonalInsightStatus.SAFE_DEFAULT,
                    new PensionInsightPageResponse.PersonalInsight(
                            latest.getPensionInsightIntro(),
                            latest.getPensionInsightStrategy()
                    )
            );
        }

        return new PensionInsightPageResponse(
                forecast,
                marketSummary,
                userInterests,
                PersonalInsightStatus.GENERATING,
                null
        );
    }

    private PensionInsightPageResponse.Forecast toForecast(PensionForecastFacts facts) {
        BigDecimal expectedMinAmount = facts.settledAmountThisMonth()
                .add(facts.expectedAdditionalAmountConservative());
        BigDecimal expectedAmount = facts.expectedTotalAmountThisMonth();
        BigDecimal expectedMaxAmount = facts.expectedMaxTotalAmountThisMonth();

        BigDecimal ongoingExpectedAmount = null;
        BigDecimal ongoingExpectedMaxAmount = null;
        if (facts.hasOngoingRoundThisMonth()
                && facts.expectedAdditionalAmountConservative() != null
                && facts.expectedAdditionalAmountBestCase() != null) {
            ongoingExpectedAmount = facts.expectedAdditionalAmountConservative();
            ongoingExpectedMaxAmount = facts.expectedAdditionalAmountBestCase();
        }

        return new PensionInsightPageResponse.Forecast(
                facts.currentPensionBalance(),
                facts.settledAmountThisMonth(),
                expectedMinAmount,
                expectedAmount,
                expectedMaxAmount,
                ongoingExpectedAmount,
                ongoingExpectedMaxAmount,
                facts.nextDepositDate()
        );
    }

    private PensionInsightPageResponse.MarketSummary findMarketSummary() {
        LocalDate today = LocalDate.now();
        MarketDailySnapshotVO snapshot = marketDailySnapshotMapper.findLatestSnapshotWithMarketSummary(
                today.minusDays(14),
                today.minusDays(1)
        );

        if (snapshot == null) {
            return new PensionInsightPageResponse.MarketSummary(
                    DEFAULT_MARKET_HEADLINE,
                    DEFAULT_MARKET_DETAIL
            );
        }

        return new PensionInsightPageResponse.MarketSummary(
                snapshot.getMarketHeadlineText(),
                snapshot.getMarketDetailText()
        );
    }

    private PensionInsightPageResponse.UserInterests findUserInterests(String userId) {
        return new PensionInsightPageResponse.UserInterests(
                findInterestNames(userId, true).stream()
                        .filter(name -> !NO_INVESTMENT_INTEREST_LABEL.equals(name))
                        .toList(),
                findInterestNames(userId, false)
        );
    }

    private List<String> findInterestNames(String userId, boolean investment) {
        return interestMapper.findUserInterestsByInvestment(userId, investment)
                .stream()
                .map(InterestVO::getInterestName)
                .filter(StringUtils::hasText)
                .toList();
    }

    private boolean isSameDayLivePassed(RecommendationVO recommendation, Long latestSurveyResultId) {
        return recommendation != null
                && latestSurveyResultId != null
                && latestSurveyResultId.equals(recommendation.getSurveyResultId())
                && recommendation.getGenerationMode() == GenerationMode.LIVE
                && recommendation.getGuardrailStatus() == GuardrailStatus.PASSED
                && isArtifactFreeRecommendation(recommendation);
    }

    private boolean isSameDaySafeDefault(RecommendationVO recommendation, Long latestSurveyResultId) {
        return recommendation != null
                && latestSurveyResultId != null
                && latestSurveyResultId.equals(recommendation.getSurveyResultId())
                && recommendation.getGenerationMode() == GenerationMode.SAFE_DEFAULT;
    }

    private boolean isArtifactFreeRecommendation(RecommendationVO recommendation) {
        try {
            ModelOutputArtifactValidator.validateRecommendationResponse(
                    RecommendationMapper.toResponse(recommendation)
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
