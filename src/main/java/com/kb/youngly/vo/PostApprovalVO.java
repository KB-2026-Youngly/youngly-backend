package com.kb.youngly.vo;

import com.kb.youngly.enums.ApprovalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostApprovalVO {
    private Long postApprovalId;
    private String userId;
    private Long postId;
    private ApprovalStatus approvalStatus;
    private String rejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
