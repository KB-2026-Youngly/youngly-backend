package com.kb.youngly.service;

import com.kb.youngly.dto.survey.SurveyAnswerDTO;
import com.kb.youngly.dto.survey.SurveySubmitRequestDTO;
import com.kb.youngly.dto.survey.SurveySubmitResponseDTO;
import com.kb.youngly.mapper.SurveyMapper;
import com.kb.youngly.mapper.UserMapper;
import com.kb.youngly.vo.survey.SurveyResultVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SurveyService 점수 합산 및 Baseline 분류 로직 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
class SurveyServiceTest {

    private static final String USER_ID = "test-user-01";

    @Mock
    private SurveyMapper surveyMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private SurveyService surveyService;

    @Test
    @DisplayName("총점 9점 설문 제출 시 baseline은 신중한 저축형이다")
    void submitSurvey_totalScore9_returnsStableBaseline() {
        // 1+1+1+2+2+2 = 9
        List<Integer> scores = Arrays.asList(1, 1, 1, 2, 2, 2);
        stubMapperForSubmit(scores);

        SurveySubmitResponseDTO response = surveyService.submitSurvey(USER_ID, buildRequest());

        assertEquals(9, response.getTotalScore());
        assertEquals("신중한 저축형", response.getBaseline());

        ArgumentCaptor<SurveyResultVO> resultCaptor = ArgumentCaptor.forClass(SurveyResultVO.class);
        verify(surveyMapper).insertSurveyResult(resultCaptor.capture());
        assertEquals(9, resultCaptor.getValue().getTotalScore());
        assertEquals("신중한 저축형", resultCaptor.getValue().getBaseline());
    }

    @Test
    @DisplayName("총점 26점 설문 제출 시 baseline은 과감한 도전형이다")
    void submitSurvey_totalScore26_returnsVeryAggressiveBaseline() {
        // 5+5+5+5+5+1 = 26
        List<Integer> scores = Arrays.asList(5, 5, 5, 5, 5, 1);
        stubMapperForSubmit(scores);

        SurveySubmitResponseDTO response = surveyService.submitSurvey(USER_ID, buildRequest());

        assertEquals(26, response.getTotalScore());
        assertEquals("과감한 도전형", response.getBaseline());

        ArgumentCaptor<SurveyResultVO> resultCaptor = ArgumentCaptor.forClass(SurveyResultVO.class);
        verify(surveyMapper).insertSurveyResult(resultCaptor.capture());
        assertEquals(26, resultCaptor.getValue().getTotalScore());
        assertEquals("과감한 도전형", resultCaptor.getValue().getBaseline());
    }

    private SurveySubmitRequestDTO buildRequest() {
        SurveySubmitRequestDTO request = new SurveySubmitRequestDTO();
        request.setAnswers(Arrays.asList(
                answer(1, 101L),
                answer(2, 102L),
                answer(3, 103L),
                answer(4, 104L),
                answer(5, 105L),
                answer(6, 106L)
        ));
        return request;
    }

    private SurveyAnswerDTO answer(int questionNo, long choiceId) {
        SurveyAnswerDTO dto = new SurveyAnswerDTO();
        dto.setQuestionNo(questionNo);
        dto.setChoiceId(choiceId);
        return dto;
    }

    private void stubMapperForSubmit(List<Integer> scores) {
        when(surveyMapper.countChoiceByQuestionNoAndChoiceId(anyInt(), anyLong())).thenReturn(1);

        for (int i = 0; i < scores.size(); i++) {
            int questionNo = i + 1;
            long choiceId = 100L + questionNo;
            when(surveyMapper.selectChoiceScore(questionNo, choiceId)).thenReturn(scores.get(i));
        }

        doAnswer(invocation -> {
            SurveyResultVO result = invocation.getArgument(0);
            result.setSurveyResultId(1L);
            return 1;
        }).when(surveyMapper).insertSurveyResult(any(SurveyResultVO.class));
    }
}
