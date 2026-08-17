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
public class CreateGroupRequest {

    private String moimAccountId;

    private String groupName;

    private Integer groupCount;

    private String customRule;

    private ChallengeType challengeType;

    private String content;

    private String futureDepositRatioRule;

    private Integer durationDays;

    private Integer minCount;

    private Integer roundCycleDays;

    private BigDecimal baseDepositAmount;


    // 프론트 추가 사항
    private Integer defaultFailPassCount;
}