package com.kb.youngly.dto.survey;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 설문 결과 상세 정보를 담는 DTO.
 * survey_results 테이블과 매핑된다.
 */
@Getter
@Setter
public class SurveyResultDTO {
    /** 설문 결과 ID (survey_results.survey_result_id) */
    private Long surveyResultId;

    /** 총점 (survey_results.total_score) */
    private Integer totalScore;

    /** 계산된 투자 성향 기준값 (survey_results.baseline) */
    private String baseline;

    /** 설문 제출 시각 (survey_results.submitted_at) */
    private LocalDateTime submittedAt;

    /** 점수 및 성향 계산 완료 시각 (survey_results.calculated_at) */
    private LocalDateTime calculatedAt;
}