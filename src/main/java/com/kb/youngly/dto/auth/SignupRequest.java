package com.kb.youngly.dto.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SignupRequest {

    private String loginId;
    private String password;
    private String name;
    private String nickname;
    private String email;

}