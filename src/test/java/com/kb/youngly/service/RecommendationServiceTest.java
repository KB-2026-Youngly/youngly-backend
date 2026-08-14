package com.kb.youngly.service;

import com.kb.youngly.client.OpenAiClient;
import com.kb.youngly.dto.recommendation.PensionForecastFacts;
import com.kb.youngly.dto.recommendation.RecommendationPrompt;
import com.kb.youngly.dto.recommendation.RecommendationResponse;
import com.kb.youngly.dto.survey.SurveyChoiceDTO;
import com.kb.youngly.dto.survey.SurveyQuestionDTO;
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
import com.kb.youngly.vo.survey.InterestVO;
import com.kb.youngly.vo.user.RecommendationVO;
import com.kb.youngly.vo.user.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

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

        service = new RecommendationService(
                userMapper,
                surveyMapper,
                interestMapper,
                marketDailySnapshotMapper,
                recommendationMapper,
                new RecommendationPromptBuilder(),
                openAiClientProvider,
                pensionForecastService,
                new RecommendationResponseQualityValidator(),
                new NumericGuardrailValidator()
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
        Long surveyResultId = 100L;
        when(openAiClient.generateRecommendation(any())).thenReturn(validLlmText());

        RecommendationResponse response = service.generateRecommendation("user01", surveyResultId);

        assertEquals(GenerationMode.LIVE, response.generationMode());
        assertEquals(GuardrailStatus.PASSED, response.guardrailStatus());
        assertEquals(new BigDecimal("30000.00"), response.settledAmountThisMonth());

        ArgumentCaptor<RecommendationVO> captor = ArgumentCaptor.forClass(RecommendationVO.class);
        verify(recommendationMapper).insert(captor.capture());
        RecommendationVO saved = captor.getValue();
        assertEquals(GenerationMode.LIVE, saved.getGenerationMode());
        assertEquals(surveyResultId, saved.getSurveyResultId());
        assertEquals(GuardrailStatus.PASSED, saved.getGuardrailStatus());
        assertEquals("user01", saved.getUserId());
        assertNotNull(saved.getForecastBasisJson());
        assertNotNull(saved.getMarketHighlightsJson());
        assertNotNull(saved.getNextDepositDate());
        verify(openAiClient, times(1)).generateRecommendation(any());
    }

    @Test
    void retriesOnceWhenQualityValidationFailsThenStoresValidLiveResponse() {
        Long surveyResultId = 100L;
        when(openAiClient.generateRecommendation(any()))
                .thenReturn(introWithoutPeriodResponse())
                .thenReturn(validLlmText());

        RecommendationResponse response = service.generateRecommendation("user01", surveyResultId);

        assertEquals(GenerationMode.LIVE, response.generationMode());
        assertEquals(GuardrailStatus.PASSED, response.guardrailStatus());

        ArgumentCaptor<RecommendationPrompt> promptCaptor = ArgumentCaptor.forClass(RecommendationPrompt.class);
        verify(openAiClient, times(2)).generateRecommendation(promptCaptor.capture());
        assertFalse(promptCaptor.getAllValues().get(0).userMessage().contains("재생성 보정 지시"));
        assertTrue(promptCaptor.getAllValues().get(1).userMessage().contains("재생성 보정 지시"));
        assertTrue(promptCaptor.getAllValues().get(1).userMessage().contains("처음부터 새로 작성하세요"));
        assertTrue(promptCaptor.getAllValues().get(1).userMessage().contains("pensionInsightIntro는 60~220자"));
        assertTrue(promptCaptor.getAllValues().get(1).userMessage().contains("pensionInsightStrategy는 60~220자"));
        assertTrue(promptCaptor.getAllValues().get(1).userMessage().contains("예상 적립액"));
        assertTrue(promptCaptor.getAllValues().get(1).userMessage().contains("JSON 외 텍스트를 출력하지 마세요"));

        ArgumentCaptor<RecommendationVO> savedCaptor = ArgumentCaptor.forClass(RecommendationVO.class);
        verify(recommendationMapper).insert(savedCaptor.capture());
        assertEquals(GenerationMode.LIVE, savedCaptor.getValue().getGenerationMode());
        assertEquals(validIntro(), savedCaptor.getValue().getPensionInsightIntro());
    }

    @Test
    void storesLiveWhenSecondResponseStillHasOnlySoftQualityFailure() {
        when(openAiClient.generateRecommendation(any()))
                .thenReturn(introWithoutPeriodResponse())
                .thenReturn(introWithoutPeriodResponse());

        RecommendationResponse response = service.generateRecommendation("user01", 100L);

        assertEquals(GenerationMode.LIVE, response.generationMode());
        assertEquals(GuardrailStatus.PASSED, response.guardrailStatus());
        verify(openAiClient, times(2)).generateRecommendation(any());
        verify(recommendationMapper).insert(any());
    }

    @Test
    void returnsSafeDefaultWhenSecondResponseHasHardQualityFailure() {
        when(openAiClient.generateRecommendation(any()))
                .thenReturn(introWithoutPeriodResponse())
                .thenReturn(internalTermResponse());

        RecommendationResponse response = service.generateRecommendation("user01", 100L);

        assertEquals(GenerationMode.SAFE_DEFAULT, response.generationMode());
        assertEquals(GuardrailStatus.FAILED_FALLBACK, response.guardrailStatus());
        verify(openAiClient, times(2)).generateRecommendation(any());
        verify(recommendationMapper).insert(any());
    }

    @Test
    void promptIncludesSurveyQuestionAnswersAndFiltersNoInvestmentInterest() {
        SurveyResultVO survey = new SurveyResultVO();
        survey.setSurveyResultId(10L);
        survey.setUserId("user01");
        survey.setBaseline("AGGRESSIVE");
        survey.setAnswersJson("{\"Q1\":4,\"Q2\":4,\"Q3\":3,\"Q4\":4,\"Q5\":3,\"Q6\":4}");
        when(surveyMapper.selectBySurveyResultIdAndUserId(10L, "user01")).thenReturn(survey);
        when(surveyMapper.selectSurveyQuestionsWithChoices()).thenReturn(List.of(
                question(1, "퇴직 후 연금 운용에서 가장 중요하게 보는 것은 무엇인가요?", 101L, 4, "장기 성장 가능성을 중요하게 봅니다."),
                question(2, "금융상품에 대해 얼마나 알고 계신가요?", 102L, 4, "수익률과 리스크를 비교할 수 있습니다."),
                question(3, "돈을 모으는 목적은 무엇인가요?", 103L, 3, "중기 목적을 준비합니다."),
                question(4, "수익과 안정성 중 무엇이 더 중요한가요?", 104L, 4, "수익성이 더 중요합니다."),
                question(5, "얼마 동안 모을 계획이신가요?", 105L, 3, "1~3년 정도입니다."),
                question(6, "손실이 발생하면 어느 정도까지 감내할 수 있나요?", 106L, 4, "변동성을 감수하고 수익을 추구합니다.")
        ));
        when(interestMapper.findUserInterestsByInvestment("user01", true))
                .thenReturn(List.of(
                        InterestVO.builder().interestName("해당 없음").build(),
                        InterestVO.builder().interestName("IT").build()
                ));
        when(openAiClient.generateRecommendation(any())).thenReturn(validLlmText());

        service.generateRecommendation("user01", 10L);

        ArgumentCaptor<RecommendationPrompt> promptCaptor = ArgumentCaptor.forClass(RecommendationPrompt.class);
        verify(openAiClient).generateRecommendation(promptCaptor.capture());
        String userMessage = promptCaptor.getValue().userMessage();

        assertTrue(userMessage.contains("퇴직 후 연금 운용"));
        assertTrue(userMessage.contains("Q1. 퇴직 후 연금 운용에서 가장 중요하게 보는 것은 무엇인가요? → 장기 성장 가능성을 중요하게 봅니다."));
        assertTrue(userMessage.contains("장기 성장 가능성"));
        assertTrue(userMessage.contains("IT"));
        assertFalse(userMessage.contains("해당 없음"));
    }

    @Test
    void consecutiveSurveyGenerationStoresResultForProvidedSurveyResultIdOnly() {
        SurveyResultVO surveyA = new SurveyResultVO();
        surveyA.setSurveyResultId(2007L);
        surveyA.setUserId("user01");
        surveyA.setBaseline("NEUTRAL");

        SurveyResultVO surveyB = new SurveyResultVO();
        surveyB.setSurveyResultId(2008L);
        surveyB.setUserId("user01");
        surveyB.setBaseline("AGGRESSIVE");

        when(surveyMapper.selectBySurveyResultIdAndUserId(2008L, "user01")).thenReturn(surveyB);
        when(openAiClient.generateRecommendation(any())).thenReturn(validLlmText());

        service.generateRecommendation("user01", 2008L);

        ArgumentCaptor<RecommendationVO> captor = ArgumentCaptor.forClass(RecommendationVO.class);
        verify(recommendationMapper).insert(captor.capture());
        assertEquals(2008L, captor.getValue().getSurveyResultId());
        assertEquals(GenerationMode.LIVE, captor.getValue().getGenerationMode());
        verify(surveyMapper, never()).selectBySurveyResultIdAndUserId(2007L, "user01");
    }

    @Test
    void returnsSafeDefaultWhenOpenAiThrows() {
        when(openAiClient.generateRecommendation(any()))
                .thenThrow(new IllegalStateException("parse failed"));

        RecommendationResponse response = service.generateRecommendation("user01", 100L);

        assertEquals(GenerationMode.SAFE_DEFAULT, response.generationMode());
        assertEquals(GuardrailStatus.FAILED_FALLBACK, response.guardrailStatus());
        assertEquals(new BigDecimal("30000.00"), response.settledAmountThisMonth());
        verify(openAiClient, times(1)).generateRecommendation(any());
        verify(recommendationMapper).insert(any());
    }

    @Test
    void returnsSafeDefaultWhenNumericGuardrailFails() {
        when(openAiClient.generateRecommendation(any())).thenReturn(
                RecommendationResponse.llmTextOnly(
                        List.of("미국 지수 상승", "채권 금리 안정", "원달러 횡보", "원자재 혼조"),
                        "a".repeat(160),
                        validIntro(),
                        "적극적인 성장형 성향을 바탕으로 분산 투자와 꾸준한 적립을 장기 관점에서 이어가며 월별 점검과 관심 분야의 변동성, 999999원 목표도 함께 정기적으로 점검하세요."
                )
        );

        RecommendationResponse response = service.generateRecommendation("user01", 100L);

        assertEquals(GenerationMode.SAFE_DEFAULT, response.generationMode());
        assertEquals(GuardrailStatus.FAILED_FALLBACK, response.guardrailStatus());
        assertDoesNotThrow(() -> service.generateRecommendation("user01", 100L));
        verify(openAiClient, times(2)).generateRecommendation(any());
    }

    @Test
    void returnsSafeDefaultAndDoesNotStoreInternalTermResponseWhenOpenAiQualityValidationFails() {
        when(openAiClient.generateRecommendation(any())).thenReturn(internalTermResponse());

        RecommendationResponse response = service.generateRecommendation("user01", 100L);

        assertEquals(GenerationMode.SAFE_DEFAULT, response.generationMode());
        ArgumentCaptor<RecommendationVO> captor = ArgumentCaptor.forClass(RecommendationVO.class);
        verify(recommendationMapper).insert(captor.capture());
        assertNotEquals("baselineLabel인 적극적인 성장형", captor.getValue().getPensionInsightIntro());
        verify(openAiClient, times(2)).generateRecommendation(any());
    }

    @Test
    void returnsSafeDefaultAndDoesNotStoreTruncatedIntroWhenOpenAiQualityValidationFails() {
        when(openAiClient.generateRecommendation(any())).thenReturn(
                RecommendationResponse.llmTextOnly(
                        List.of("미국 지수 상승", "채권 금리 안정", "원달러 횡보", "원자재 혼조"),
                        "최근 시장은 주요 지수와 금리 흐름이 교차하며 변동성이 이어지고 있어요. "
                                + "개인연금 관점에서는 단기 등락보다 장기 흐름을 차분히 살펴보는 태도가 중요해요.",
                        "고객님은 2~3년의 투자 경험과 금융상품의 수익률·위험을 비교할 수 있다는 응답을 바탕으로 '적극적인 성장형' 성향에 가깝게 나타났어요. "
                                + "1~3년의 계획과 변동성을 감수하고 수익을 추구한다는 기준은 IT/테크와 반도체에 대한 관심과도 연결되지만, 개인연금은 단기 흐름보다 장기 자산배분과 분산의 원칙 안에서 관리하는 것이",
                        validStrategy()
                )
        );

        RecommendationResponse response = service.generateRecommendation("user01", 100L);

        assertEquals(GenerationMode.SAFE_DEFAULT, response.generationMode());
        ArgumentCaptor<RecommendationVO> captor = ArgumentCaptor.forClass(RecommendationVO.class);
        verify(recommendationMapper).insert(captor.capture());
        assertNull(captor.getValue().getPensionInsightIntro());
        verify(openAiClient, times(2)).generateRecommendation(any());
    }

    @Test
    void returnsSafeDefaultWhenInsertFails() {
        when(openAiClient.generateRecommendation(any())).thenReturn(validLlmText());
        when(recommendationMapper.insert(any()))
                .thenThrow(new RuntimeException("db down"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.generateRecommendation("user01", 100L)
        );
        assertTrue(exception.getMessage().contains("추천 결과 저장에 실패했습니다"));
    }

    @Test
    void convertsArtifactResponseToSafeDefaultBeforeInsert() {
        when(openAiClient.generateRecommendation(any())).thenReturn(
                RecommendationResponse.llmTextOnly(
                        List.of("미국 지수 상승", "채권 금리 안정", "원달러 횡보", "원자재 혼조"),
                        "최근 시장은 주요 지수와 금리 흐름이 교차하며 변동성이 이어지고 있어요. "
                                + "개인연금 관점에서는 단기 등락보다 장기 흐름을 차분히 살펴보는 태도가 중요해요.",
                        validIntro(),
                        "연금 계좌의 자산군 비중을 확인해 현재의 안정성 수준이 부담 없이 유지되는지 살펴보세요. ”} } }```</analysis> 0 0 0 0 0 0 0 0 0 0"
                )
        );

        RecommendationResponse response = service.generateRecommendation("user01", 100L);

        assertEquals(GenerationMode.SAFE_DEFAULT, response.generationMode());
        assertEquals(GuardrailStatus.FAILED_FALLBACK, response.guardrailStatus());

        ArgumentCaptor<RecommendationVO> captor = ArgumentCaptor.forClass(RecommendationVO.class);
        verify(recommendationMapper).insert(captor.capture());
        assertEquals(GenerationMode.SAFE_DEFAULT, captor.getValue().getGenerationMode());
        assertEquals(GuardrailStatus.FAILED_FALLBACK, captor.getValue().getGuardrailStatus());
        assertFalse(captor.getValue().getPensionInsightStrategy().contains("</analysis>"));
    }

    @Test
    void throwsSurveyNotCompletedWhenSurveyResultMissing() {
        when(surveyMapper.selectBySurveyResultIdAndUserId(100L, "user01")).thenReturn(null);

        SurveyNotCompletedException exception = assertThrows(
                SurveyNotCompletedException.class,
                () -> service.generateRecommendation("user01", 100L)
        );

        assertEquals(
                "설문 결과를 찾을 수 없거나 투자성향이 없습니다. userId=user01, surveyResultId=100",
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
                () -> service.generateRecommendation(unknownUserId, 100L)
        );

        assertEquals(
                "사용자를 찾을 수 없습니다. userId=" + unknownUserId,
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

    private void stubUserAndSurvey(String userId) {
        UserVO user = new UserVO();
        user.setUserId(userId);
        user.setBirthday(LocalDate.of(1999, 5, 20));
        when(userMapper.findByUserId(userId)).thenReturn(user);

        SurveyResultVO survey = new SurveyResultVO();
        survey.setUserId(userId);
        survey.setSurveyResultId(100L);
        survey.setBaseline("AGGRESSIVE");
        when(surveyMapper.selectLatestResultByUserId(userId)).thenReturn(survey);
        when(surveyMapper.selectBySurveyResultIdAndUserId(100L, userId)).thenReturn(survey);
    }

    private static SurveyQuestionDTO question(int questionNo, String questionText, long choiceId, int displayOrder, String choiceText) {
        SurveyQuestionDTO question = new SurveyQuestionDTO();
        question.setQuestionNo(questionNo);
        question.setQuestionText(questionText);

        SurveyChoiceDTO choice = new SurveyChoiceDTO();
        choice.setChoiceId(choiceId);
        choice.setDisplayOrder(displayOrder);
        choice.setChoiceText(choiceText);
        question.setChoices(List.of(choice));
        return question;
    }

    private static RecommendationResponse validLlmText() {
        return RecommendationResponse.llmTextOnly(
                List.of("미국 지수 상승", "채권 금리 안정", "원달러 횡보", "원자재 혼조"),
                "최근 시장은 주요 지수와 금리 흐름이 교차하며 변동성이 이어지고 있어요. "
                        + "개인연금 관점에서는 단기 등락보다 장기 흐름을 차분히 살펴보는 태도가 중요해요.",
                validIntro(),
                validStrategy()
        );
    }

    private static RecommendationResponse internalTermResponse() {
        return RecommendationResponse.llmTextOnly(
                List.of("미국 지수 상승", "채권 금리 안정", "원달러 횡보", "원자재 혼조"),
                "최근 시장은 주요 지수와 금리 흐름이 교차하며 변동성이 이어지고 있어요. "
                        + "개인연금 관점에서는 단기 등락보다 장기 흐름을 차분히 살펴보는 태도가 중요해요.",
                "고객님은 baselineLabel인 적극적인 성장형과 투자관심 분야인 IT/테크, 반도체를 반영해 개인연금 인사이트를 구성합니다. "
                        + "2~3년의 투자 경험과 금융상품의 수익률·위험을 비교할 수 있다는 응답을 바탕으로 장기 자산배분과 분산의 원칙 안에서 관리하는 것이 중요해요.",
                validStrategy()
        );
    }

    private static RecommendationResponse introWithoutPeriodResponse() {
        return RecommendationResponse.llmTextOnly(
                List.of("미국 지수 상승", "채권 금리 안정", "원달러 횡보", "원자재 혼조"),
                "최근 시장은 주요 지수와 금리 흐름이 교차하며 변동성이 이어지고 있어요. "
                        + "개인연금 관점에서는 단기 등락보다 장기 흐름을 차분히 살펴보는 태도가 중요해요.",
                "고객님은 2~3년의 투자 경험과 금융상품의 수익률·위험을 비교할 수 있다는 응답을 바탕으로 '적극적인 성장형' 성향에 가깝게 나타났어요. "
                        + "1~3년의 계획과 변동성을 감수하고 수익을 추구한다는 기준은 IT/테크와 반도체에 대한 관심과도 연결되지만, 개인연금은 단기 흐름보다 장기 자산배분과 분산의 원칙 안에서 더 차분히 관리하는 것이 중요해요",
                validStrategy()
        );
    }

    private static String validIntro() {
        return "2~3년의 투자 경험과 1~3년 계획, 변동성 감수 응답을 보면 적극적인 성장형 성향이 드러나요. "
                + "개인연금은 단기 흐름보다 장기 관리가 중요하므로 IT/테크 관심도 분산 원칙 안에서 점검해 보세요.";
    }

    private static String validStrategy() {
        return "IT/테크 관심은 분산 원칙으로 나누고 장기 점검 일정을 정해 매달 한 번씩 변동 흐름을 차분히 살펴보세요.";
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
