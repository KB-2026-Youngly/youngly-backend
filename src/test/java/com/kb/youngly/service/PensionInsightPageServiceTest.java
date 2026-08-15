package com.kb.youngly.service;

import com.kb.youngly.dto.recommendation.PensionForecastFacts;
import com.kb.youngly.dto.recommendation.PensionInsightPageResponse;
import com.kb.youngly.enums.Baseline;
import com.kb.youngly.enums.GenerationMode;
import com.kb.youngly.enums.GuardrailStatus;
import com.kb.youngly.enums.PersonalInsightStatus;
import com.kb.youngly.mapper.InterestMapper;
import com.kb.youngly.mapper.MarketDailySnapshotMapper;
import com.kb.youngly.mapper.RecommendationMapper;
import com.kb.youngly.mapper.SurveyMapper;
import com.kb.youngly.mapper.UserMapper;
import com.kb.youngly.vo.market.MarketDailySnapshotVO;
import com.kb.youngly.vo.survey.InterestVO;
import com.kb.youngly.vo.survey.SurveyResultVO;
import com.kb.youngly.vo.user.RecommendationVO;
import com.kb.youngly.vo.user.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PensionInsightPageServiceTest {

    private UserMapper userMapper;
    private SurveyMapper surveyMapper;
    private InterestMapper interestMapper;
    private MarketDailySnapshotMapper marketDailySnapshotMapper;
    private RecommendationMapper recommendationMapper;
    private PensionForecastService pensionForecastService;
    private PensionInsightPageService service;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        surveyMapper = mock(SurveyMapper.class);
        interestMapper = mock(InterestMapper.class);
        marketDailySnapshotMapper = mock(MarketDailySnapshotMapper.class);
        recommendationMapper = mock(RecommendationMapper.class);
        pensionForecastService = mock(PensionForecastService.class);

        service = new PensionInsightPageService(
                userMapper,
                surveyMapper,
                interestMapper,
                marketDailySnapshotMapper,
                recommendationMapper,
                pensionForecastService
        );

        UserVO user = new UserVO();
        user.setUserId("user01");
        when(userMapper.findByUserId("user01")).thenReturn(user);
        when(pensionForecastService.calculateForecast("user01")).thenReturn(sampleFacts("user01"));
        when(interestMapper.findUserInterestsByInvestment(eq("user01"), eq(true)))
                .thenReturn(List.of(InterestVO.builder().interestName("해당 없음").build()));
        when(interestMapper.findUserInterestsByInvestment(eq("user01"), eq(false)))
                .thenReturn(List.of(InterestVO.builder().interestName("운동").build()));

        MarketDailySnapshotVO snapshot = new MarketDailySnapshotVO();
        snapshot.setMarketHeadlineText("금리 흐름 안정");
        snapshot.setMarketDetailText("최근 시장동향은 금리와 주요 지표를 중심으로 완만하게 정리되고 있습니다.");
        when(marketDailySnapshotMapper.findLatestSnapshotWithMarketSummary(any(), any()))
                .thenReturn(snapshot);
    }

    @Test
    void surveyMissingReturnsSurveyRequiredWithUnlockedCommonSections() {
        when(surveyMapper.selectLatestResultByUserId("user01")).thenReturn(null);

        PensionInsightPageResponse response = service.getInsightPage("user01");

        assertEquals(PersonalInsightStatus.SURVEY_REQUIRED, response.personalInsightStatus());
        assertNull(response.personalInsight());
        assertNotNull(response.forecast());
        assertEquals("금리 흐름 안정", response.marketSummary().headline());
        assertEquals(List.of(), response.userInterests().investment());
        assertEquals(List.of("운동"), response.userInterests().general());
    }

    @Test
    void surveyExistsWithoutCacheReturnsGenerating() {
        when(surveyMapper.selectLatestResultByUserId("user01")).thenReturn(survey());
        when(recommendationMapper.selectLatestByUserId("user01")).thenReturn(null);

        PensionInsightPageResponse response = service.getInsightPage("user01");

        assertEquals(PersonalInsightStatus.GENERATING, response.personalInsightStatus());
        assertNull(response.personalInsight());
    }

    @Test
    void latestRecommendationForPreviousSurveyReturnsGenerating() {
        when(surveyMapper.selectLatestResultByUserId("user01")).thenReturn(survey());
        when(recommendationMapper.selectLatestByUserId("user01"))
                .thenReturn(recommendation(99L, GenerationMode.LIVE, GuardrailStatus.PASSED));

        PensionInsightPageResponse response = service.getInsightPage("user01");

        assertEquals(PersonalInsightStatus.GENERATING, response.personalInsightStatus());
        assertNull(response.personalInsight());
    }

    @Test
    void sameDayLivePassedCacheReturnsReady() {
        when(surveyMapper.selectLatestResultByUserId("user01")).thenReturn(survey());
        when(recommendationMapper.selectLatestByUserId("user01"))
                .thenReturn(recommendation(100L, GenerationMode.LIVE, GuardrailStatus.PASSED));

        PensionInsightPageResponse response = service.getInsightPage("user01");

        assertEquals(PersonalInsightStatus.READY, response.personalInsightStatus());
        assertEquals("개인화 인사이트", response.personalInsight().intro());
    }

    @Test
    void sameDaySafeDefaultCacheReturnsSafeDefault() {
        when(surveyMapper.selectLatestResultByUserId("user01")).thenReturn(survey());
        when(recommendationMapper.selectLatestByUserId("user01"))
                .thenReturn(recommendation(100L, GenerationMode.SAFE_DEFAULT, GuardrailStatus.FAILED_FALLBACK));

        PensionInsightPageResponse response = service.getInsightPage("user01");

        assertEquals(PersonalInsightStatus.SAFE_DEFAULT, response.personalInsightStatus());
        assertNotNull(response.personalInsight());
    }

    @Test
    void missingUserKeepsNotFoundBehavior() {
        when(userMapper.findByUserId("missing")).thenReturn(null);

        assertThrows(NoSuchElementException.class, () -> service.getInsightPage("missing"));
    }

    private static SurveyResultVO survey() {
        SurveyResultVO survey = new SurveyResultVO();
        survey.setSurveyResultId(100L);
        survey.setBaseline("AGGRESSIVE");
        return survey;
    }

    private static RecommendationVO recommendation(Long surveyResultId, GenerationMode mode, GuardrailStatus status) {
        return RecommendationVO.builder()
                .userId("user01")
                .surveyResultId(surveyResultId)
                .baseline(Baseline.AGGRESSIVE)
                .pensionInsightIntro("개인화 인사이트")
                .pensionInsightStrategy("분산 투자와 꾸준한 적립을 장기 관점에서 살펴보세요.")
                .generationMode(mode)
                .guardrailStatus(status)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private static PensionForecastFacts sampleFacts(String userId) {
        return new PensionForecastFacts(
                userId,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                new BigDecimal("30000.00"),
                new BigDecimal("150000.00"),
                new BigDecimal("250000.00"),
                new BigDecimal("180000.00"),
                new BigDecimal("280000.00"),
                new BigDecimal("5030000.00"),
                true,
                LocalDate.of(2026, 8, 10),
                List.of()
        );
    }
}
