package com.kb.youngly.dto.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SignupRequest {

    private String loginId;
    private String password;
    private String name;
    private String nickname;
    private String email;
    private LocalDate birthday;

    /**
     * 사용자가 선택한 일반 관심사 ID 목록
     * 예: interests.is_investment = false 인 데이터의 interest_id
     */
    private List<Long> interestIds;

    /**
     * 사용자가 선택한 투자 관심 카테고리 ID 목록
     * 예: interests.is_investment = true 인 데이터의 interest_id
     */
    private List<Long> investmentInterestIds;

}