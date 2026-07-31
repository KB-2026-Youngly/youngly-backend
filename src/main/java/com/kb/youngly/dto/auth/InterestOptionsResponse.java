package com.kb.youngly.dto.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 회원가입 화면의 관심사 선택지 목록 응답 DTO
 * 일반 관심사와 투자 관심 카테고리를 구분해서 전달한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class InterestOptionsResponse {

    /** 일반 관심사 목록 */
    private List<InterestOptionDTO> interests;

    /** 투자 관심 카테고리 목록 */
    private List<InterestOptionDTO> investmentInterests;
}
