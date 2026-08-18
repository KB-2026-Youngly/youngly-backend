package com.kb.youngly.dto.posts;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostApprovalRequestDTO {

    private String approvalStatus;

    private String rejectReason;
}