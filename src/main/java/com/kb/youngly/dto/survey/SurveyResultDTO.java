package com.kb.youngly.dto.survey;

import com.kb.youngly.dto.auth.InterestOptionDTO;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 최신 설문 결과 조회 응답 DTO.
 * survey_results + survey_result_interest 조인 결과를 표현한다.
 */
@Getter
@Setter
public class SurveyResultDTO {
    /** 설문 결과 ID (survey_results.survey_result_id) */
    private Long surveyResultId;

    /** 총점 (survey_results.total_score) */
    private Integer totalScore;

    /** 계산된 투자 성향 (survey_results.baseline, 한글 유형명) */
    private String baseline;

    /** 설문 제출 시각 (survey_results.submitted_at) */
    private LocalDateTime submittedAt;

    /** 점수 및 성향 계산 완료 시각 (survey_results.calculated_at) */
    private LocalDateTime calculatedAt;

    /** 투자 관심산업 (interests.is_investment = true) */
    private List<InterestOptionDTO> investmentInterestList;

    /** 취미/관심사 (interests.is_investment = false) */
    private List<InterestOptionDTO> hobbyInterestList;
}
