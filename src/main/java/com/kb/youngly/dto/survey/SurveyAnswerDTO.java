package com.kb.youngly.dto.survey;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SurveyAnswerDTO {
    /** 답변한 문항 ID (survey_questions.question_id) */
    private Long questionId;

    /** 사용자가 선택한 선택지 ID (survey_choices.choice_id) */
    private Long choiceId;
}
