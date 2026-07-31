package com.kb.youngly.dto.point;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 사용자의 현재 포인트 잔액 응답입니다.
 */
@Getter
@AllArgsConstructor
public class PointBalanceResponse {
    private long balance;
}
