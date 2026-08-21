package com.kb.youngly.dto.posts;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class PostApprovalResponseDTO {
    private String approvalStatus;
    private int earnedPoint;
    private long currentPoint;
    private String message;
}
