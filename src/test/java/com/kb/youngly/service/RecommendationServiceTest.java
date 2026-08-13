package com.kb.youngly.service;

import com.kb.youngly.client.OpenAiClient;
import com.kb.youngly.dto.recommendation.PensionForecastFacts;
import com.kb.youngly.dto.recommendation.RecommendationResponse;
import com.kb.youngly.enums.GenerationMode;
import com.kb.youngly.enums.GuardrailStatus;
import com.kb.youngly.exception.SurveyNotCompletedException;
import com.kb.youngly.mapper.InterestMapper;
import com.kb.youngly.mapper.MarketDailySnapshotMapper;
import com.kb.youngly.mapper.RecommendationMapper;
import com.kb.youngly.mapper.SurveyMapper;
import com.kb.youngly.mapper.UserMapper;
import com.kb.youngly.prompt.RecommendationPromptBuilder;
import com.kb.youngly.vo.survey.SurveyResultVO;
import com.kb.youngly.vo.user.RecommendationVO;
import com.kb.youngly.vo.user.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RecommendationServiceTest {

    private UserMapper userMapper;
    private SurveyMapper surveyMapper;
    private InterestMapper interestMapper;
    private MarketDailySnapshotMapper marketDailySnapshotMapper;
    private RecommendationMapper recommendationMapper;
    private OpenAiClient openAiClient;
    private ObjectProvider<OpenAiClient> openAiClientProvider;
    private PensionForecastService pensionForecastService;
    private RecommendationService service;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        surveyMapper = mock(SurveyMapper.class);
        interestMapper = mock(InterestMapper.class);
        marketDailySnapshotMapper = mock(MarketDailySnapshotMapper.class);
        recommendationMapper = mock(RecommendationMapper.class);
        openAiClient = mock(OpenAiClient.class);
        openAiClientProvider = mock(ObjectProvider.class);
        pensionForecastService = mock(PensionForecastService.class);

        when(openAiClientProvider.getObject()).thenReturn(openAiClient);

        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});

        service = new RecommendationService(
                userMapper,
                surveyMapper,
                interestMapper,
                marketDailySnapshotMapper,
                recommendationMapper,
                new RecommendationPromptBuilder(),
                openAiClientProvider,
                mock(MarketSnapshotIngestService.class),
                pensionForecastService,
                new NumericGuardrailValidator(),
                environment
        );

        stubUserAndSurvey("user01");
        when(interestMapper.findUserInterestsByInvestment(eq("user01"), anyBoolean()))
                .thenReturn(List.of());
        when(marketDailySnapshotMapper.findRecentSnapshots(any(), any(), anyInt()))
                .thenReturn(List.of());
        when(pensionForecastService.calculateForecast("user01"))
                .thenReturn(sampleFacts("user01"));
        when(recommendationMapper.insert(any())).thenReturn(1);
    }

    @Test
    void savesValidatedLiveResponseAsPassed() {
        when(openAiClient.generateRecommendation(any())).thenReturn(validLlmText());

        RecommendationResponse response = service.generateRecommendation("user01");

        assertEquals(GenerationMode.LIVE, response.generationMode());
        assertEquals(GuardrailStatus.PASSED, response.guardrailStatus());
        assertEquals(new BigDecimal("30000.00"), response.settledAmountThisMonth());

        ArgumentCaptor<RecommendationVO> captor = ArgumentCaptor.forClass(RecommendationVO.class);
        verify(recommendationMapper).insert(captor.capture());
        RecommendationVO saved = captor.getValue();
        assertEquals(GenerationMode.LIVE, saved.getGenerationMode());
        assertEquals(GuardrailStatus.PASSED, saved.getGuardrailStatus());
        assertEquals("user01", saved.getUserId());
        assertNotNull(saved.getForecastBasisJson());
        assertNotNull(saved.getMarketHighlightsJson());
        assertNotNull(saved.getNextDepositDate());
    }

    @Test
    void returnsSafeDefaultWhenOpenAiThrows() {
        when(openAiClient.generateRecommendation(any()))
                .thenThrow(new IllegalStateException("parse failed"));

        RecommendationResponse response = service.generateRecommendation("user01");

        assertEquals(GenerationMode.SAFE_DEFAULT, response.generationMode());
        assertEquals(GuardrailStatus.FAILED_FALLBACK, response.guardrailStatus());
        assertEquals(new BigDecimal("30000.00"), response.settledAmountThisMonth());
        verify(recommendationMapper).insert(any());
    }

    @Test
    void returnsSafeDefaultWhenNumericGuardrailFails() {
        when(openAiClient.generateRecommendation(any())).thenReturn(
                RecommendationResponse.llmTextOnly(
                        List.of("미국 지수 상승", "채권 금리 안정", "원달러 횡보", "원자재 혼조"),
                        "a".repeat(160),
                        "고객님의 투자 성향은 적극적인 성장형에 가까워요. 개인연금은 장기 자산입니다.",
                        "분산 투자와 꾸준한 적립으로 999999원 목표를 만드세요."
                )
        );

        RecommendationResponse response = service.generateRecommendation("user01");

        assertEquals(GenerationMode.SAFE_DEFAULT, response.generationMode());
        assertEquals(GuardrailStatus.FAILED_FALLBACK, response.guardrailStatus());
        assertDoesNotThrow(() -> service.generateRecommendation("user01"));
    }

    @Test
    void returnsSafeDefaultWhenInsertFails() {
        when(openAiClient.generateRecommendation(any())).thenReturn(validLlmText());
        when(recommendationMapper.insert(any()))
                .thenThrow(new RuntimeException("db down"));

        RecommendationResponse response = assertDoesNotThrow(
                () -> service.generateRecommendation("user01")
        );

        assertEquals(GenerationMode.SAFE_DEFAULT, response.generationMode());
        assertEquals(GuardrailStatus.FAILED_FALLBACK, response.guardrailStatus());
        assertEquals(new BigDecimal("180000.00"), response.expectedTotalAmountThisMonth());
    }

    @Test
    void throwsSurveyNotCompletedWhenSurveyResultMissing() {
        when(surveyMapper.selectLatestResultByUserId("user01")).thenReturn(null);

        SurveyNotCompletedException exception = assertThrows(
                SurveyNotCompletedException.class,
                () -> service.generateRecommendation("user01")
        );

        assertEquals(
                "사용자의 최신 투자성향 설문 결과가 없습니다. userId=user01",
                exception.getMessage()
        );

        verifyNoInteractions(
                interestMapper,
                marketDailySnapshotMapper,
                recommendationMapper,
                openAiClient,
                pensionForecastService
        );
    }

    @Test
    void throwsNotFoundWhenUserDoesNotExist() {
        String unknownUserId = "unknown-user";

        when(userMapper.findByUserId(unknownUserId)).thenReturn(null);

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> service.generateRecommendation(unknownUserId)
        );

        assertEquals(
                "사용자를 찾을 수 없습니다. userId=" + unknownUserId,
                exception.getMessage()
        );

        verifyNoInteractions(
                surveyMapper,
                interestMapper,
                marketDailySnapshotMapper,
                recommendationMapper,
                openAiClient,
                pensionForecastService
        );
    }

    private void stubUserAndSurvey(String userId) {
        UserVO user = new UserVO();
        user.setUserId(userId);
        user.setBirthday(LocalDate.of(1999, 5, 20));
        when(userMapper.findByUserId(userId)).thenReturn(user);

        SurveyResultVO survey = new SurveyResultVO();
        survey.setUserId(userId);
        survey.setBaseline("AGGRESSIVE");
        when(surveyMapper.selectLatestResultByUserId(userId)).thenReturn(survey);
    }

    private static RecommendationResponse validLlmText() {
        return RecommendationResponse.llmTextOnly(
                List.of("미국 지수 상승", "채권 금리 안정", "원달러 횡보", "원자재 혼조"),
                "최근 시장은 주요 지수와 금리 흐름이 교차하며 변동성이 이어지고 있어요. "
                        + "개인연금 관점에서는 단기 등락보다 장기 흐름을 차분히 살펴보는 태도가 중요해요.",
                "고객님의 투자 성향은 적극적인 성장형에 가까워요. 개인연금은 장기 자산입니다.",
                "분산 투자와 꾸준한 적립을 바탕으로 장기 목표에 맞는 자산을 만들어 가 보세요."
        );
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
