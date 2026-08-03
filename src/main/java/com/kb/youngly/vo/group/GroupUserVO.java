package com.kb.youngly.vo;

import com.kb.youngly.enums.GroupUserStatus;
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
public class GroupUserVO {
    private Long groupUserId;
    private String groupId;
    private String userId;
    private GroupUserStatus groupUserStatus;
    private LocalDateTime approvedAt;
    private BigDecimal currentDepositAmount;
    private Integer streakCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
