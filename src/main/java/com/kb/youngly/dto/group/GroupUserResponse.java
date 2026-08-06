package com.kb.youngly.dto.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupUserResponse {

    private Long groupUserId;

    private String userId;

    private String nickname;

    private String profileImageUrl;

    private boolean leader;
}