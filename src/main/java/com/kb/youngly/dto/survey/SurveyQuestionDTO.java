package com.kb.youngly.dto.survey;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 설문 문항 정보를 담는 DTO.
 * survey_questions + survey_choices 조인 결과를 표현한다.
 */
@Getter
@Setter
public class SurveyQuestionDTO {
    /** 문항 ID (survey_questions.question_id) */
    private Long questionId;

    /** 문항 번호 (survey_questions.question_no, UNIQUE) */
    private Integer questionNo;

    /** 문항 내용 (survey_questions.question_text) */
    private String questionText;

    /** 다중 선택 가능 여부 (survey_questions.is_multiple) */
    private Boolean multiple;

    /** 해당 문항의 선택지 목록 (survey_choices) */
    private List<SurveyChoiceDTO> choices;
}