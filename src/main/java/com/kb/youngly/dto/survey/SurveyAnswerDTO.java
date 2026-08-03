package com.kb.youngly.dto.survey;

import lombok.Getter;
import lombok.Setter;

/**
 * 설문 제출 시 문항별 응답 바디.
 * 요청 JSON: { "questionNo": 1, "choiceId": 2 }
 */
@Getter
@Setter
public class SurveyAnswerDTO {
    /** 문항 번호 (survey_questions.question_no) */
    private Integer questionNo;

    /** 사용자가 선택한 선택지 ID (survey_choices.choice_id) */
    private Long choiceId;
}
