package com.kb.youngly.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserResponse {

    private String userId;
    private String name;
    private String loginId;
    private String nickname;
    private String email;
    private String profileImageUrl;
    private Long point;
}