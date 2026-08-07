package com.kb.youngly.dto.group;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RegenerateInviteCodeResponse {

    private String inviteCode;
}