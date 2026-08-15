package com.kb.youngly.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.youngly.client.OpenAiClient;
import com.kb.youngly.dto.recommendation.GuardrailResult;
import com.kb.youngly.dto.recommendation.PensionForecastFacts;
import com.kb.youngly.dto.recommendation.RecommendationPrompt;
import com.kb.youngly.dto.recommendation.RecommendationPromptContext;
import com.kb.youngly.dto.recommendation.RecommendationResponse;
import com.kb.youngly.dto.recommendation.SurveyQuestionAnswer;
import com.kb.youngly.dto.survey.SurveyAnswerJsonDTO;
import com.kb.youngly.dto.survey.SurveyChoiceDTO;
import com.kb.youngly.dto.survey.SurveyQuestionDTO;
import com.kb.youngly.enums.Baseline;
import com.kb.youngly.enums.GenerationMode;
import com.kb.youngly.enums.GuardrailStatus;
import com.kb.youngly.exception.RecommendationQualityValidationException;
import com.kb.youngly.exception.SurveyNotCompletedException;
import com.kb.youngly.mapper.InterestMapper;
import com.kb.youngly.mapper.MarketDailySnapshotMapper;
import com.kb.youngly.mapper.RecommendationMapper;
import com.kb.youngly.mapper.SurveyMapper;
import com.kb.youngly.mapper.UserMapper;
import com.kb.youngly.prompt.RecommendationPromptBuilder;
import com.kb.youngly.util.ModelOutputArtifactValidator;
import com.kb.youngly.vo.market.MarketDailySnapshotVO;
import com.kb.youngly.vo.survey.InterestVO;
import com.kb.youngly.vo.survey.SurveyResultVO;
import com.kb.youngly.vo.user.UserVO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private static final Logger log = LogManager.getLogger(RecommendationService.class);

    private static final int MARKET_CONTEXT_MAX_LENGTH = 4_000;
    private static final int RAW_TEXT_PER_DAY_MAX_LENGTH = 500;
    private static final String NO_INVESTMENT_INTEREST_LABEL = "해당 없음";
    private static final int MAX_LLM_ATTEMPTS = 2;

    private static final String QUALITY_RETRY_INSTRUCTION = """

            [재생성 보정 지시]
            직전 출력은 사용자 노출 문구 검증에 실패했습니다.
            직전 답변을 고치거나 늘리지 말고, 처음부터 새로 작성하세요.

            pensionInsightIntro는 60~220자 내외의 자연스러운 존댓말 1~2문장을 권장합니다.
            intro는 설문 답안을 요약하는 대신, 해당 성향에서 개인연금을 어떤 관점으로 바라보면 좋은지 설명하세요.
            설문 응답을 문장 순서대로 번역하거나 나열하지 말고 실제 응답의 의미를 자연스럽게 연결하세요.
            "당신은"으로 문장을 시작하지 마세요.
            개인연금을 비상금, 단기 생활비, 비상 상황 대비 자금처럼 표현하지 마세요.
            FACTS에 원금보장·원금보호 정보가 없으면 "원금 보호"라는 표현을 쓰지 마세요.

            pensionInsightStrategy는 60~220자 내외의 자연스러운 존댓말 1~2문장을 권장합니다.
            strategy에는 하나의 확인 대상과 하나의 판단 기준을 명확히 쓰세요.
            예: "연금 계좌의 자산군별 비중을 확인해, 현재 위험 수준이 부담스럽지 않은지 살펴보세요."
            실제 계좌 구성 FACTS가 없으면 특정 자산군 이름 대신 "연금 계좌의 자산군별 비중"처럼 확인 대상만 쓰세요.
            서로 다른 행동을 두 개 제안하지 말고, 사용자가 바로 확인할 한 가지 행동만 제시하세요.
            챌린지, 정산, 예상 적립액, 최대 적립액, 입금 예정일, 계좌 잔액은 언급하지 마세요.

            "당신은", "투자 성향에 맞춰", "연금 운용 원칙으로", "정보 탐색이 필요할 수 있습니다",
            "스스로의 감당력", "권합니다", "도움이 됩니다", "점이 반영됩니다", "해당 분야",
            "금융 이해도 기초", "조건이 허용된다면"은 쓰지 마세요.
            "필요할 수 있습니다", "도움이 됩니다", "권합니다"처럼 구체적인 정보나 행동이 없는 표현을 쓰지 마세요.
            "조건이 허용된다면", "도움이 됩니다"처럼 조건이나 대상이 불명확한 표현을 쓰지 마세요.
            "투자 성향에 맞춰", "연금 운용 원칙으로", "스스로의 감당력"처럼 설명문·상담 보고서 같은 표현을 쓰지 마세요.
            작성 규칙, 단어 개수, 문장 개수, 길이 조건, 포함 조건, "분산 2개",
            "자연스럽게 포함", "한 가지로 좁히되" 같은 프롬프트 지시를 답변에 절대 쓰지 마세요.
            JSON 외 텍스트를 출력하지 마세요.
            사용자 FACTS와 금융 안전 규칙은 변경하지 말고 그대로 따르세요.
            """;

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final UserMapper userMapper;
    private final SurveyMapper surveyMapper;
    private final InterestMapper interestMapper;
    private final MarketDailySnapshotMapper marketDailySnapshotMapper;
    private final RecommendationMapper recommendationMapper;
    private final RecommendationPromptBuilder promptBuilder;
    private final ObjectProvider<OpenAiClient> openAiClientProvider;
    private final PensionForecastService pensionForecastService;
    private final RecommendationResponseQualityValidator qualityValidator;
    private final NumericGuardrailValidator guardrailValidator;

    public RecommendationService(
            UserMapper userMapper,
            SurveyMapper surveyMapper,
            InterestMapper interestMapper,
            MarketDailySnapshotMapper marketDailySnapshotMapper,
            RecommendationMapper recommendationMapper,
            RecommendationPromptBuilder promptBuilder,
            ObjectProvider<OpenAiClient> openAiClientProvider,
            PensionForecastService pensionForecastService,
            RecommendationResponseQualityValidator qualityValidator,
            NumericGuardrailValidator guardrailValidator
    ) {
        this.userMapper = userMapper;
        this.surveyMapper = surveyMapper;
        this.interestMapper = interestMapper;
        this.marketDailySnapshotMapper = marketDailySnapshotMapper;
        this.recommendationMapper = recommendationMapper;
        this.promptBuilder = promptBuilder;
        this.openAiClientProvider = openAiClientProvider;
        this.pensionForecastService = pensionForecastService;
        this.qualityValidator = qualityValidator;
        this.guardrailValidator = guardrailValidator;
    }

    /**
     * 신규 설문 결과 ID를 기준으로 추천을 생성한다.
     * 최신 설문을 다시 조회하지 않아 비동기 실행 중 설문이 바뀌어도 엉뚱한 결과를 저장하지 않는다.
     */
    public RecommendationResponse generateRecommendation(
            String userId,
            Long surveyResultId
    ) {
        UserVO user = userMapper.findByUserId(userId);

        if (user == null) {
            throw new NoSuchElementException(
                    "사용자를 찾을 수 없습니다. userId=" + userId
            );
        }

        SurveyResultVO surveyResult = surveyMapper
                .selectBySurveyResultIdAndUserId(surveyResultId, userId);

        if (surveyResult == null || !StringUtils.hasText(surveyResult.getBaseline())) {
            throw new SurveyNotCompletedException(
                    "설문 결과를 찾을 수 없거나 투자성향이 없습니다. userId="
                            + userId + ", surveyResultId=" + surveyResultId
            );
        }

        PensionForecastFacts facts = pensionForecastService.calculateForecast(userId);

        String ageBand = resolveAgeBand(user.getBirthday());
        String baseline = surveyResult.getBaseline();
        String baselineLabel = resolveBaselineLabel(baseline);

        List<String> investmentInterests =
                normalizeInvestmentInterests(findInterestNames(userId, true));

        List<String> hobbyInterests =
                findInterestNames(userId, false);

        RecentMarketContext marketContext = buildRecentMarketContextWithFallback();

        List<SurveyQuestionAnswer> surveyQuestionAnswers =
                resolveSurveyQuestionAnswers(surveyResult);

        RecommendationPromptContext context = new RecommendationPromptContext(
                userId,
                ageBand,
                baseline,
                baselineLabel,
                investmentInterests,
                hobbyInterests,
                surveyQuestionAnswers,
                buildSurveyQuestionAnswerSummary(
                        surveyQuestionAnswers,
                        baseline,
                        baselineLabel
                ),
                marketContext.promptText(),
                facts
        );

        RecommendationPrompt prompt = promptBuilder.build(context);

        RecommendationResponse response = null;
        GuardrailResult lastResult = null;

        for (int attempt = 1; attempt <= MAX_LLM_ATTEMPTS; attempt++) {
            try {
                RecommendationPrompt attemptPrompt = attempt == 1
                        ? prompt
                        : withQualityRetryInstruction(prompt);

                RecommendationResponse candidate = openAiClientProvider
                        .getObject()
                        .generateRecommendation(attemptPrompt);

                RecommendationResponse candidateWithFacts = candidate.withFacts(
                        facts,
                        GenerationMode.LIVE,
                        GuardrailStatus.PASSED
                );

                RecommendationQualityValidationException softQualityFailure = null;
                try {
                    qualityValidator.validate(candidateWithFacts, facts);
                } catch (RecommendationQualityValidationException validationException) {
                    log.warn(
                            "[RECOMMENDATION_QUALITY_VALIDATION_FAILED] attempt={}/{}, hard={}, type={}, field={}, length={}, lastChar={}, bannedTerm={}",
                            attempt,
                            MAX_LLM_ATTEMPTS,
                            validationException.isHardFailure(),
                            validationException.getFailureType(),
                            validationException.getFieldName(),
                            validationException.getLength(),
                            validationException.getLastChar(),
                            validationException.getBannedTerm()
                    );

                    if (validationException.isHardFailure()) {
                        throw validationException;
                    }

                    softQualityFailure = validationException;
                    if (attempt < MAX_LLM_ATTEMPTS) {
                        continue;
                    }
                }

                lastResult = guardrailValidator.validate(candidateWithFacts, facts);

                if (lastResult.passed()) {
                    response = candidateWithFacts;
                    if (softQualityFailure != null) {
                        log.warn(
                                "[RECOMMENDATION_SOFT_QUALITY_ACCEPTED] attempt={}, type={}, field={}",
                                attempt,
                                softQualityFailure.getFailureType(),
                                softQualityFailure.getFieldName()
                        );
                    }
                    break;
                }

                log.warn(
                        "[WARN] 추천 숫자/금융 가드레일 검증 실패 "
                                + "(attempt={}/{}). reason={}",
                        attempt,
                        MAX_LLM_ATTEMPTS,
                        lastResult.reason()
                );

                break;
            } catch (RecommendationQualityValidationException e) {
                lastResult = GuardrailResult.failed(e.getMessage());

                log.warn(
                        "[WARN] 추천 응답 하드 품질 검증 실패 "
                                + "(attempt={}/{}). type={}, field={}, length={}, "
                                + "lastChar={}, bannedTerm={}",
                        attempt,
                        MAX_LLM_ATTEMPTS,
                        e.getFailureType(),
                        e.getFieldName(),
                        e.getLength(),
                        e.getLastChar(),
                        e.getBannedTerm()
                );
                break;
            } catch (Exception e) {
                lastResult = GuardrailResult.failed(e.getMessage());

                log.warn(
                        "[WARN] 추천 LLM 호출/파싱 실패 "
                                + "(attempt={}/{}). errorType={}, reason={}",
                        attempt,
                        MAX_LLM_ATTEMPTS,
                        e.getClass().getSimpleName(),
                        e.getMessage(),
                        e
                );

                break;
            }
        }

        if (response == null) {
            log.warn(
                    "[WARN] {}회 재시도 후에도 가드레일 통과 실패. "
                            + "텍스트 필드는 비우고 facts만 반환합니다. reason={}",
                    MAX_LLM_ATTEMPTS,
                    lastResult == null ? null : lastResult.reason()
            );

            response = createSafeDefaultReport(marketContext, facts, baselineLabel, investmentInterests)
                    .withFacts(
                            facts,
                            GenerationMode.SAFE_DEFAULT,
                            GuardrailStatus.FAILED_FALLBACK
                    );
        }

        log.info(
                "[RECOMMENDATION_MARKET_CONTEXT_RESOLVED] userId={}, surveyResultId={}, snapshotCount={}, hasHeadline={}, hasDetail={}",
                userId,
                surveyResultId,
                marketContext.snapshotCount(),
                marketContext.hasHeadline(),
                marketContext.hasDetail()
        );

        response = ensurePersistableResponse(
                userId,
                surveyResultId,
                response,
                marketContext,
                facts,
                baselineLabel,
                investmentInterests
        );

        saveRecommendation(userId, baseline, surveyResultId, response);

        return response;
    }

    /**
     * 예상하지 못한 비동기 예외가 발생했을 때도 recommendation 행을 남겨
     * 조회 API가 GENERATING 상태에 영구히 남지 않도록 한다.
     */
    public void saveSafeDefaultOnUnexpectedFailure(
            String userId,
            Long surveyResultId,
            Exception cause
    ) {
        try {
            SurveyResultVO surveyResult = surveyMapper
                    .selectBySurveyResultIdAndUserId(surveyResultId, userId);

            if (surveyResult == null || !StringUtils.hasText(surveyResult.getBaseline())) {
                log.error(
                        "[RECOMMENDATION_FALLBACK_SKIP] 설문 결과가 없습니다. "
                                + "userId={}, surveyResultId={}",
                        userId,
                        surveyResultId,
                        cause
                );
                return;
            }

            RecommendationResponse fallback;
            try {
                PensionForecastFacts facts = pensionForecastService.calculateForecast(userId);
                RecentMarketContext marketContext = buildRecentMarketContextWithFallback();
                fallback = createSafeDefaultReport(
                                marketContext,
                                facts,
                                resolveBaselineLabel(surveyResult.getBaseline()),
                                List.of()
                        )
                        .withFacts(
                                facts,
                                GenerationMode.SAFE_DEFAULT,
                                GuardrailStatus.FAILED_FALLBACK
                        );
            } catch (Exception forecastException) {
                log.warn("[RECOMMENDATION_FALLBACK_FORECAST_FAILED] userId={}, surveyResultId={}",
                        userId, surveyResultId, forecastException);
                fallback = safeDefaultWithoutForecastFacts();
            }

            saveRecommendation(
                    userId,
                    surveyResult.getBaseline(),
                    surveyResultId,
                    fallback
            );

            log.warn(
                    "[RECOMMENDATION_FALLBACK_SAVED] userId={}, surveyResultId={}",
                    userId,
                    surveyResultId
            );
        } catch (Exception fallbackException) {
            log.error(
                    "[RECOMMENDATION_FALLBACK_SAVE_FAILED] userId={}, surveyResultId={}",
                    userId,
                    surveyResultId,
                    fallbackException
            );
        }
    }

    private void saveRecommendation(
            String userId,
            String baseline,
            Long surveyResultId,
            RecommendationResponse response
    ) {
        try {
            ModelOutputArtifactValidator.validateRecommendationResponse(response);
            recommendationMapper.insert(
                    RecommendationMapper.toVO(
                            userId,
                            baseline,
                            surveyResultId,
                            response
                    )
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "추천 결과 저장에 실패했습니다. userId="
                            + userId + ", surveyResultId=" + surveyResultId,
                    e
            );
        }
    }

    private RecommendationResponse ensurePersistableResponse(
            String userId,
            Long surveyResultId,
            RecommendationResponse response,
            RecentMarketContext marketContext,
            PensionForecastFacts facts,
            String baselineLabel,
            List<String> investmentInterests
    ) {
        try {
            ModelOutputArtifactValidator.validateRecommendationResponse(response);
            return response;
        } catch (RecommendationQualityValidationException artifactException) {
            log.error(
                    "[RECOMMENDATION_ARTIFACT_BEFORE_INSERT] userId={}, surveyResultId={}, type={}, field={}, bannedTerm={}",
                    userId,
                    surveyResultId,
                    artifactException.getFailureType(),
                    artifactException.getFieldName(),
                    artifactException.getBannedTerm(),
                    artifactException
            );

            return createSafeDefaultReport(marketContext, facts, baselineLabel, investmentInterests)
                    .withFacts(
                            facts,
                            GenerationMode.SAFE_DEFAULT,
                            GuardrailStatus.FAILED_FALLBACK
                    );
        }
    }

    private String resolveAgeBand(LocalDate birthday) {
        if (birthday == null) {
            return "미상";
        }

        int age = Period.between(birthday, LocalDate.now()).getYears();
        int ageBand = Math.max(age / 10 * 10, 0);

        return ageBand + "대";
    }

    private RecommendationPrompt withQualityRetryInstruction(
            RecommendationPrompt prompt
    ) {
        return new RecommendationPrompt(
                prompt.systemMessage(),
                prompt.userMessage() + QUALITY_RETRY_INSTRUCTION
        );
    }

    private String resolveBaselineLabel(String baselineValue) {
        try {
            return Baseline.valueOf(baselineValue).getLabel();
        } catch (IllegalArgumentException e) {
            for (Baseline baseline : Baseline.values()) {
                if (baseline.getLabel().equals(baselineValue)) {
                    return baseline.getLabel();
                }
            }

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

    private List<String> normalizeInvestmentInterests(List<String> interests) {
        List<String> filtered = interests.stream()
                .filter(name -> !NO_INVESTMENT_INTEREST_LABEL.equals(name))
                .toList();

        return filtered.isEmpty() ? List.of() : filtered;
    }

    private List<SurveyQuestionAnswer> resolveSurveyQuestionAnswers(
            SurveyResultVO surveyResult
    ) {
        if (surveyResult == null || !StringUtils.hasText(surveyResult.getAnswersJson())) {
            return List.of();
        }

        try {
            List<SurveyAnswerJsonDTO> answers = JSON_MAPPER.readValue(
                    surveyResult.getAnswersJson(),
                    JSON_MAPPER.getTypeFactory().constructCollectionType(
                            List.class,
                            SurveyAnswerJsonDTO.class
                    )
            );

            Map<Integer, SurveyQuestionDTO> questionsByNo =
                    loadSurveyQuestionsByNo();

            return toSurveyQuestionAnswers(answers, questionsByNo, false);
        } catch (Exception e) {
            try {
                List<SurveyAnswerJsonDTO> answers =
                        parseLegacyQuestionMapAnswers(surveyResult.getAnswersJson());

                Map<Integer, SurveyQuestionDTO> questionsByNo =
                        loadSurveyQuestionsByNo();

                return toSurveyQuestionAnswers(answers, questionsByNo, true);
            } catch (Exception legacyException) {
                log.warn(
                        "[WARN] 설문 문항/답변 프롬프트 컨텍스트 구성 실패. "
                                + "baseline만 사용합니다. surveyResultId={}",
                        surveyResult.getSurveyResultId(),
                        legacyException
                );

                return List.of();
            }
        }
    }

    private Map<Integer, SurveyQuestionDTO> loadSurveyQuestionsByNo() {
        return surveyMapper.selectSurveyQuestionsWithChoices()
                .stream()
                .collect(Collectors.toMap(
                        SurveyQuestionDTO::getQuestionNo,
                        Function.identity()
                ));
    }

    private List<SurveyAnswerJsonDTO> parseLegacyQuestionMapAnswers(
            String answersJson
    ) throws Exception {
        JsonNode root = JSON_MAPPER.readTree(answersJson);

        if (!root.isObject()) {
            return List.of();
        }

        List<SurveyAnswerJsonDTO> answers = new ArrayList<>();

        root.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();

            if (key == null
                    || !key.matches("Q\\d+")
                    || !value.canConvertToInt()) {
                return;
            }

            int questionNo = Integer.parseInt(key.substring(1));
            long displayOrder = value.asLong();

            answers.add(
                    new SurveyAnswerJsonDTO(
                            questionNo,
                            displayOrder,
                            null
                    )
            );
        });

        return answers;
    }

    private List<SurveyQuestionAnswer> toSurveyQuestionAnswers(
            List<SurveyAnswerJsonDTO> answers,
            Map<Integer, SurveyQuestionDTO> questionsByNo,
            boolean choiceValueIsDisplayOrder
    ) {
        List<SurveyQuestionAnswer> result = new ArrayList<>();

        for (SurveyAnswerJsonDTO answer : answers) {
            if (answer.getQuestionNo() == null || answer.getChoiceId() == null) {
                continue;
            }

            SurveyQuestionDTO question =
                    questionsByNo.get(answer.getQuestionNo());

            if (question == null || question.getChoices() == null) {
                continue;
            }

            String choiceText = question.getChoices()
                    .stream()
                    .filter(choice -> choiceValueIsDisplayOrder
                            ? choice.getDisplayOrder() != null
                            && answer.getChoiceId().intValue()
                            == choice.getDisplayOrder()
                            : answer.getChoiceId().equals(choice.getChoiceId()))
                    .findFirst()
                    .map(SurveyChoiceDTO::getChoiceText)
                    .orElse(null);

            if (!StringUtils.hasText(question.getQuestionText())
                    || !StringUtils.hasText(choiceText)) {
                continue;
            }

            result.add(
                    new SurveyQuestionAnswer(
                            answer.getQuestionNo(),
                            abbreviate(question.getQuestionText(), 160),
                            abbreviate(choiceText, 160)
                    )
            );
        }

        return result;
    }

    private String buildSurveyQuestionAnswerSummary(
            List<SurveyQuestionAnswer> answers,
            String baseline,
            String baselineLabel
    ) {
        StringBuilder builder = new StringBuilder("[투자성향 설문 결과]");

        for (SurveyQuestionAnswer answer : answers) {
            builder.append("\nQ")
                    .append(answer.questionNo())
                    .append(". ")
                    .append(answer.questionText())
                    .append(" → ")
                    .append(answer.choiceText());
        }

        builder.append("\n최종 성향: ")
                .append(baselineLabel)
                .append(" (")
                .append(baseline)
                .append(")");

        return builder.toString();
    }

    private RecentMarketContext buildRecentMarketContextWithFallback() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(14);
        LocalDate end = today.minusDays(1);

        List<MarketDailySnapshotVO> snapshots =
                marketDailySnapshotMapper.findRecentSnapshots(start, end, 20);

        StringBuilder builder = new StringBuilder();
        MarketDailySnapshotVO latestTextSnapshot = null;
        boolean hasHeadline = false;
        boolean hasDetail = false;

        for (MarketDailySnapshotVO snapshot : snapshots) {
            String text = resolveSnapshotText(snapshot);

            if (!StringUtils.hasText(text)) {
                continue;
            }

            if (latestTextSnapshot == null) {
                latestTextSnapshot = snapshot;
            }

            hasHeadline = hasHeadline || StringUtils.hasText(snapshot.getMarketHeadlineText());
            hasDetail = hasDetail || StringUtils.hasText(snapshot.getMarketDetailText());

            appendWithinLimit(
                    builder,
                    "[" + snapshot.getMarketDate() + "] " + text.trim()
            );

            if (builder.length() >= MARKET_CONTEXT_MAX_LENGTH) {
                break;
            }
        }

        return new RecentMarketContext(
                builder.toString(),
                resolveMarketHighlights(latestTextSnapshot),
                resolveMarketDetail(latestTextSnapshot),
                snapshots.size(),
                hasHeadline,
                hasDetail
        );
    }

    private String resolveSnapshotText(MarketDailySnapshotVO snapshot) {
        if (StringUtils.hasText(snapshot.getMarketHeadlineText())
                || StringUtils.hasText(snapshot.getMarketDetailText())) {
            return String.join(
                    "\n",
                    nullToEmpty(snapshot.getMarketHeadlineText()),
                    nullToEmpty(snapshot.getMarketDetailText())
            ).trim();
        }

        if (StringUtils.hasText(snapshot.getSummaryText())) {
            return snapshot.getSummaryText();
        }

        return abbreviate(
                snapshot.getRawText(),
                RAW_TEXT_PER_DAY_MAX_LENGTH
        );
    }

    private void appendWithinLimit(
            StringBuilder builder,
            String text
    ) {
        String textToAppend = builder.length() > 0
                ? "\n\n" + text
                : text;

        int remaining = MARKET_CONTEXT_MAX_LENGTH - builder.length();

        if (remaining > 0) {
            builder.append(
                    textToAppend,
                    0,
                    Math.min(textToAppend.length(), remaining)
            );
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

    private List<String> resolveMarketHighlights(MarketDailySnapshotVO snapshot) {
        if (snapshot == null) {
            return List.of();
        }

        if (StringUtils.hasText(snapshot.getMarketHeadlineText())) {
            return splitHeadline(snapshot.getMarketHeadlineText());
        }

        String fallback = resolveSnapshotText(snapshot);
        if (!StringUtils.hasText(fallback)) {
            return List.of();
        }

        return List.of(abbreviate(fallback, 80));
    }

    private List<String> splitHeadline(String headline) {
        return List.of(headline.split("\\R+"))
                .stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(line -> line.replaceFirst("^[\\-•*]\\s*", ""))
                .limit(4)
                .toList();
    }

    private String resolveMarketDetail(MarketDailySnapshotVO snapshot) {
        if (snapshot == null) {
            return null;
        }

        if (StringUtils.hasText(snapshot.getMarketDetailText())) {
            return snapshot.getMarketDetailText();
        }

        if (StringUtils.hasText(snapshot.getSummaryText())) {
            return snapshot.getSummaryText();
        }

        return abbreviate(snapshot.getRawText(), RAW_TEXT_PER_DAY_MAX_LENGTH);
    }

    private RecommendationResponse createSafeDefaultReport(
            RecentMarketContext marketContext,
            PensionForecastFacts facts,
            String baselineLabel,
            List<String> investmentInterests
    ) {
        return RecommendationResponse.llmTextOnly(
                marketContext == null ? List.of() : marketContext.marketHighlights(),
                marketContext == null ? null : marketContext.marketDetail(),
                buildDeterministicIntro(),
                buildDeterministicStrategy()
        );
    }

    private String buildDeterministicIntro() {
        return "개인연금은 감당 가능한 위험 수준과 장기 목표를 함께 확인하며 관리하는 것이 좋아요.";
    }

    private String buildDeterministicStrategy() {
        return "연금 계좌의 자산군별 비중을 확인해 현재 위험 수준이 부담스럽지 않은지 살펴보세요.";
    }

    private RecommendationResponse safeDefaultWithoutForecastFacts() {
        return new RecommendationResponse(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                List.of(),
                List.of(),
                null,
                buildDeterministicIntro(),
                buildDeterministicStrategy(),
                GenerationMode.SAFE_DEFAULT,
                GuardrailStatus.FAILED_FALLBACK
        );
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record RecentMarketContext(
            String promptText,
            List<String> marketHighlights,
            String marketDetail,
            int snapshotCount,
            boolean hasHeadline,
            boolean hasDetail
    ) {
    }
}
