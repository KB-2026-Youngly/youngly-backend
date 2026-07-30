package com.kb.youngly.dto.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 회원가입 화면에서 관심사/투자 관심 카테고리 선택지를 내려주기 위한 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class InterestOptionDTO {

    /**
     * 관심사 ID
     */
    private Long interestId;

    /**
     * 화면에 표시할 관심사 이름
     */
    private String interestName;

    /**
     * 투자 관심 카테고리 여부
     * - true  : 투자 관련 관심 카테고리
     * - false : 일반 관심사 카테고리
     */
    private Boolean investment;
}
