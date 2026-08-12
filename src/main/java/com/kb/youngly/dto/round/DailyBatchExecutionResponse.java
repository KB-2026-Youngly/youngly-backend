package com.kb.youngly.dto.round;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/** 개발용 수동 일일 배치 API의 단계별 실행 결과. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyBatchExecutionResponse {

    private LocalDate batchDate;
    private Integer autoApprovedPostCount;
    private Integer weeklyTargetCount;
    private Integer weeklySettledParticipantCount;
    private Integer weeklyFailedTargetCount;
    private Integer transitionTargetCount;
    private Integer transitionedGroupCount;
    private Integer transitionSkippedCount;
    private Integer transitionFailedGroupCount;
    private Integer settlementTargetCount;
    private Integer settledGroupCount;
    private Integer settlementSkippedCount;
    private Integer settlementFailedGroupCount;
    private List<String> failures;
}
