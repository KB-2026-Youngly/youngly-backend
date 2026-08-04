package com.kb.youngly.vo.user;

import com.kb.youngly.enums.UserStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {

    private String userId;
    private String name;
    private String loginId;
    private String nickname;
    private String email;
    private String profileImageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String password;
    private UserStatus userStatus;
    private Long point;
    private LocalDate birthday;
}
