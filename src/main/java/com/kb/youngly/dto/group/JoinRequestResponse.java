package com.kb.youngly.dto.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinRequestResponse {

    private Long groupUserId;

    private String userId;

    private String nickname;

    private String profileImageUrl;

}