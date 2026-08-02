package com.kb.youngly.dto.point;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 인증된 사용자의 현재 포인트 잔액만 반환합니다.
 */
@Getter
@AllArgsConstructor
public class PointBalanceResponse {
    private long balance;
}
