package com.kb.youngly.service;

import com.kb.youngly.dto.deposit.DepositRequest;
import com.kb.youngly.dto.deposit.DepositResponse;
import com.kb.youngly.dto.deposit.MemberDepositStatusResponse;

import java.util.List;

/**
 * 그룹 예치금 납부 및 현황 조회 기능.
 *
 * <p>예치금은 사용자의 개인 입출금 계좌에서 그룹 모임통장으로 이동하며,
 * 참여자별 잔액은 {@code group_users.current_deposit_amount}에 관리한다.</p>
 */
public interface DepositService {

    /**
     * 예치금을 납부한다. 요청 금액이 없으면 남은 필요 예치금 전액을 납부한다.
     *
     * @param userId  납부 사용자 ID
     * @param groupId 대상 그룹 ID
     * @param request 출금 계좌, 선택적 납부 금액, 멱등성 키
     * @return 납부 후 예치 현황
     */
    DepositResponse deposit(String userId, String groupId, DepositRequest request);

    /** 로그인 사용자의 그룹 예치금 현황을 조회한다. */
    DepositResponse getMyDeposit(String userId, String groupId);

    /** 요청자가 속한 그룹의 참여자 전체 예치 현황을 조회한다. */
    List<MemberDepositStatusResponse> getMemberDepositStatuses(
            String requesterUserId, String groupId);
}
