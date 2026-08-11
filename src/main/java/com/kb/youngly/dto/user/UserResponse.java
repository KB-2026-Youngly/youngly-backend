package com.kb.youngly.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private String userId;
    private String name;
    private String loginId;
    private String nickname;
    private String email;
    private String profileImageUrl;
    private Long point;
    private LocalDate birthday;
    private Boolean isNotificationAgreement;
}