package com.kb.youngly.dto.deposit;

import com.kb.youngly.enums.GroupUserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// 그룹 참여자별 예치금 현황 응답. 
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberDepositStatusResponse {
    // 예치 현황 대상 참여자 ID. 
    private String userId;
    // 화면에 표시할 참여자 닉네임. 
    private String nickname;
    // 참여자 프로필 이미지 경로. 이미지가 없으면 {@code null}. 
    private String profileImageUrl;
    // 그룹이 정한 참여자 1인당 기준 예치금. 
    private BigDecimal requiredAmount;
    // 참여자가 현재까지 모임통장에 납부한 예치금. 
    private BigDecimal currentDepositAmount;
    // 예치금 납부 상태를 포함한 그룹 참여 상태. 
    private GroupUserStatus groupUserStatus;
}
