package com.kb.youngly.vo;

import com.kb.youngly.enums.ChallengeType;
import com.kb.youngly.enums.GroupStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupHistoryVO {
    private Long groupHistoryId;
    private String moimAccountId;
    private String groupId;
    private Long groupHistoryVersion;
    private String inviteCode;
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
    private GroupStatus groupStatus;
    private LocalDateTime createdAt;
}
