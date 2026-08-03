package com.kb.youngly.dto.survey;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 설문 제출 응답 DTO.
 * DB의 baseline 컬럼에 한글 투자성향 유형명을 저장·반환한다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SurveySubmitResponseDTO {
    private Long surveyResultId;
    private String baseline;
    private Integer totalScore;
}
