package com.kb.youngly.dto.group;

import com.kb.youngly.enums.ChallengeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGroupRequest {

    private String groupName;

    private String customRule;

    private String content;

    private String futureDepositRatioRule;

    private Integer durationDays;

    private Integer minCount;

    private Integer roundCycleDays;

    private BigDecimal baseDepositAmount;

    private ChallengeType challengeType;
    private Integer defaultFailPassCount;
}