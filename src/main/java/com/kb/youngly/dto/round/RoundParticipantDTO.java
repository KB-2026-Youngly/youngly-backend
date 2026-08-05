package com.kb.youngly.dto.round;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 라운드 이력 초기화에 필요한 참여자 조회 결과 DTO.
 *
 * <p>그룹 참여 상태가 PENDING_DEPOSIT 또는 ACTIVE인 사용자와
 * 해당 사용자가 선택한 미래 적립금 수령 계좌를 계층 간에 전달한다.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoundParticipantDTO {

    /** 라운드에 참여할 사용자 ID. */
    private String userId;

    /** 정산된 미래 적립금을 받을 서비스 연결 계좌 ID. */
    private String accountId;
}
