package com.kb.youngly.dto.deposit;

import com.kb.youngly.enums.GroupUserStatus;
import com.kb.youngly.enums.TransactionCategory;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/** 예치금 납부 결과 또는 현재 예치 현황 응답 DTO. */
@Value
@Builder
public class DepositResponse {
    /** 예치금이 속한 그룹 ID. */
    String groupId;
    /** 예치금을 납부하거나 현황을 조회한 사용자 ID. */
    String userId;
    /** 그룹이 정한 참여자 1인당 기준 예치금. */
    BigDecimal requiredAmount;
    /** 사용자가 현재까지 납부한 누적 예치금. */
    BigDecimal depositedAmount;
    /** 기준 예치금을 채우기 위해 추가로 납부해야 하는 금액. */
    BigDecimal remainingAmount;
    /** 예치금 충족 여부를 포함한 현재 참여 상태. */
    GroupUserStatus groupUserStatus;
    /** 이번 납부의 거래 목적. 단순 현황 조회 시에는 {@code null}. */
    TransactionCategory transactionCategory;
}
