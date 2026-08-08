package com.kb.youngly.dto.group;

import com.kb.youngly.enums.ChallengeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateChallengeRequest {

    private ChallengeType challengeType;

    private String content;

    private Integer durationDays;

    private Integer minCount;

    private Integer roundCycleDays;

    private BigDecimal baseDepositAmount;

    private String futureDepositRatioRule;
}