package com.kb.youngly.dto.survey;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 설문 제출 요청 DTO.
 * 사용자가 응답한 답변 목록을 전달하고,
 * 서버에서는 이를 기반으로 survey_results를 생성한다.
 */
@Getter
@Setter
public class SurveySubmitRequestDTO {
    /** 사용자가 제출한 답변 목록 (정확히 6문항) */
    private List<SurveyAnswerDTO> answers;
}
