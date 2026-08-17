package com.kb.youngly.dto.group;

import com.kb.youngly.enums.ChallengeType;
import com.kb.youngly.enums.GroupStatus;
import com.kb.youngly.enums.GroupUserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupListResponse {

    private String groupId;
    private String groupName;
    private String moimAccountId;

    // 최대 모집 인원
    private Integer groupCount;

    // 현재 참여 인원
    private Integer memberCount;

    private ChallengeType challengeType;
    private GroupStatus groupStatus;

    // 현재 로그인 사용자의 참여 상태
    private GroupUserStatus myMembershipStatus;

    // 방장 여부
    private Boolean leader;
}