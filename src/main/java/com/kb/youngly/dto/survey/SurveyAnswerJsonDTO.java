package com.kb.youngly.dto.survey;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * survey_results.answers_json 직렬화용 DTO.
 * 저장 JSON 예: [{"questionNo":1,"choiceId":2,"score":3}, ...]
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SurveyAnswerJsonDTO {
    private Integer questionNo;
    private Long choiceId;
    private Integer score;
}
