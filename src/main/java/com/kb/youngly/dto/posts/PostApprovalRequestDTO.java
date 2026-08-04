package com.kb.youngly.dto.posts;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostApprovalRequestDTO {
    // 임시로 권한 검증을 위해 프론트에서 유저 ID를 받는다고 가정
    private String userId;

    // 'APPROVE' 또는 'REJECT' (post_approvals 테이블 기준)
    private String approvalStatus;

    // approvalStatus가 'REJECT'일 경우 필수, 'APPROVE'일 경우 null 가능
    private String rejectReason;
}