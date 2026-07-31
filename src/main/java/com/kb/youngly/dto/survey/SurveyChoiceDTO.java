package com.kb.youngly.dto.survey;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SurveyChoiceDTO {
    /** 선택지 ID (survey_choices.choice_id) */
    private Long choiceId;

    /** 선택지 내용 (survey_choices.choice_text) */
    private String choiceText;

    /** 선택 시 반영되는 점수 (survey_choices.score) */
    private Integer score;

    /** 화면 표시 순서 (survey_choices.display_order) */
    private Integer displayOrder;
}