package com.kb.youngly.dto.deposit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 그룹 예치금 납부 요청 DTO. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepositRequest {

    /** 예치금을 출금할 로그인 사용자 소유의 입출금 계좌 ID. */
    private String sourceAccountId;

    /** 이번 납부 금액. 생략하면 남은 필요 예치금 전액을 납부한다. */
    private BigDecimal amount;

    /** 중복 클릭과 네트워크 재시도에 따른 중복 납부를 막는 요청 고유 키. */
    private String idempotencyKey;
}
