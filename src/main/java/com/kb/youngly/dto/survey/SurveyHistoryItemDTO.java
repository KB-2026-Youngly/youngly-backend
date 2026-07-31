package com.kb.youngly.dto.survey;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 사용자의 설문 이력 목록에서 1건을 나타내는 DTO.
 * survey_results 테이블의 요약 정보를 사용한다.
 */
@Getter
@Setter
public class SurveyHistoryItemDTO {
    /** 설문 결과 ID (survey_results.survey_result_id) */
    private Long surveyResultId;

    /** 총점 (survey_results.total_score) */
    private Integer totalScore;

    /** 계산된 투자 성향 기준값 (survey_results.baseline) */
    private String baseline;

    /** 설문 제출 시각 (survey_results.submitted_at) */
    private LocalDateTime submittedAt;
}
