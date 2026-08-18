package com.kb.youngly.dto.recommendation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PensionForecastSource {

    private Long roundId;
    private String groupId;
    private String groupName;
    private String challengeType;
    private BigDecimal baseDepositAmount;
    private String futureDepositRatioRule;
    private Integer minCount;
    /** 진행 중 라운드의 round_history.success_count (streak_count 아님) */
    private Integer successCount;
    /** success_count 기준 RANK() 임시 순위 (updateRoundRanks와 동일 규칙) */
    private Integer rankNo;
    /** 해당 라운드에서 결산 완료된 주차 수 (weekly_settlements 중 created_at <= NOW() DISTINCT week_no) */
    private Integer completedWeekCount;
    private LocalDate roundEndDate;

    public PensionForecastSource(
            String groupId,
            String groupName,
            String challengeType,
            BigDecimal baseDepositAmount,
            String futureDepositRatioRule,
            Integer minCount,
            Integer successCount,
            Integer rankNo,
            Integer completedWeekCount,
            LocalDate roundEndDate
    ) {
        this(
                null, groupId, groupName, challengeType, baseDepositAmount,
                futureDepositRatioRule, minCount, successCount, rankNo,
                completedWeekCount, roundEndDate
        );
    }
}
