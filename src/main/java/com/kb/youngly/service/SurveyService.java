package com.kb.youngly.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.youngly.dto.auth.InterestOptionDTO;
import com.kb.youngly.dto.survey.SurveyAnswerDTO;
import com.kb.youngly.dto.survey.SurveyAnswerJsonDTO;
import com.kb.youngly.dto.survey.SurveyQuestionDTO;
import com.kb.youngly.dto.survey.SurveyResultDTO;
import com.kb.youngly.dto.survey.SurveySubmitRequestDTO;
import com.kb.youngly.dto.survey.SurveySubmitResponseDTO;
import com.kb.youngly.enums.Baseline;
import com.kb.youngly.mapper.SurveyMapper;
import com.kb.youngly.mapper.UserMapper;
import com.kb.youngly.vo.survey.SurveyResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SurveyService {

    private static final int REQUIRED_ANSWER_COUNT = 6;

    private final SurveyMapper surveyMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public List<SurveyQuestionDTO> getSurveyQuestionsWithChoices() {
        return surveyMapper.selectSurveyQuestionsWithChoices();
    }

    @Transactional
    public SurveySubmitResponseDTO submitSurvey(String userId, SurveySubmitRequestDTO request) {
        List<SurveyAnswerDTO> answers = validateAnswers(request);

        List<SurveyAnswerJsonDTO> answerJsonList = new ArrayList<>();
        for (SurveyAnswerDTO answer : answers) {
            Integer score = surveyMapper.selectChoiceScore(answer.getQuestionNo(), answer.getChoiceId());
            if (score == null) {
                throw new IllegalArgumentException(
                        "존재하지 않는 선택지입니다. questionNo=" + answer.getQuestionNo()
                                + ", choiceId=" + answer.getChoiceId());
            }
            answerJsonList.add(new SurveyAnswerJsonDTO(
                    answer.getQuestionNo(), answer.getChoiceId(), score));
        }

        int totalScore = calculateTotalScore(answerJsonList);
        String baseline = classifyInvestorType(totalScore);
        LocalDateTime now = LocalDateTime.now();

        SurveyResultVO result = new SurveyResultVO();
        result.setUserId(userId);
        result.setAnswersJson(toAnswersJson(answerJsonList));
        result.setTotalScore(totalScore);
        result.setBaseline(baseline);
        result.setSubmittedAt(now);
        result.setCalculatedAt(now);

        surveyMapper.insertSurveyResult(result);

        return new SurveySubmitResponseDTO(result.getSurveyResultId(), baseline, totalScore);
    }

    @Transactional(readOnly = true)
    public SurveyResultDTO getLatestSurveyResult(String userId) {
        SurveyResultVO result = surveyMapper.selectLatestResultByUserId(userId);
        if (result == null) {
            throw new NoSuchElementException("최신 설문 결과가 없습니다.");
        }

        List<InterestOptionDTO> interests = userMapper.selectInterestsByUserId(userId);

        SurveyResultDTO dto = new SurveyResultDTO();
        dto.setSurveyResultId(result.getSurveyResultId());
        dto.setTotalScore(result.getTotalScore());
        dto.setBaseline(result.getBaseline());
        dto.setSubmittedAt(result.getSubmittedAt());
        dto.setCalculatedAt(result.getCalculatedAt());
        dto.setInvestmentInterestList(interests.stream()
                .filter(i -> Boolean.TRUE.equals(i.getInvestment()))
                .collect(Collectors.toList()));
        dto.setHobbyInterestList(interests.stream()
                .filter(i -> !Boolean.TRUE.equals(i.getInvestment()))
                .collect(Collectors.toList()));
        return dto;
    }

    int calculateTotalScore(List<SurveyAnswerJsonDTO> answers) {
        return answers.stream()
                .mapToInt(SurveyAnswerJsonDTO::getScore)
                .sum();
    }

    String classifyInvestorType(int totalScore) {
        return Baseline.fromTotalScore(totalScore).getLabel();
    }

    private List<SurveyAnswerDTO> validateAnswers(SurveySubmitRequestDTO request) {
        if (request == null || CollectionUtils.isEmpty(request.getAnswers())) {
            throw new IllegalArgumentException("설문 응답이 비어 있습니다.");
        }

        List<SurveyAnswerDTO> answers = request.getAnswers();
        if (answers.size() != REQUIRED_ANSWER_COUNT) {
            throw new IllegalArgumentException(
                    "설문 응답은 정확히 " + REQUIRED_ANSWER_COUNT + "문항이어야 합니다.");
        }

        Set<Integer> questionNos = new HashSet<>();
        for (SurveyAnswerDTO answer : answers) {
            if (answer.getQuestionNo() == null || answer.getChoiceId() == null) {
                throw new IllegalArgumentException("questionNo와 choiceId는 필수입니다.");
            }
            if (!questionNos.add(answer.getQuestionNo())) {
                throw new IllegalArgumentException(
                        "중복된 문항 번호입니다: " + answer.getQuestionNo());
            }
            int count = surveyMapper.countChoiceByQuestionNoAndChoiceId(
                    answer.getQuestionNo(), answer.getChoiceId());
            if (count == 0) {
                throw new IllegalArgumentException(
                        "문항에 속하지 않는 선택지입니다. questionNo="
                                + answer.getQuestionNo() + ", choiceId=" + answer.getChoiceId());
            }
        }
        return answers;
    }

    private String toAnswersJson(List<SurveyAnswerJsonDTO> answers) {
        try {
            return objectMapper.writeValueAsString(answers);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("answers_json 직렬화에 실패했습니다.", e);
        }
    }
}
