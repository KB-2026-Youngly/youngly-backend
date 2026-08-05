package com.kb.youngly.dto.round;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/** 개발용 수동 주간 결산 API의 실행 결과. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklySettlementExecutionResponse {
    private LocalDate settlementDate;
    private Integer targetCount;
    private Integer failedTargetCount;
    private Integer successfulParticipantCount;
    private List<String> failures;
}
