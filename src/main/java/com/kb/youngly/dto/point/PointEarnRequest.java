package com.kb.youngly.dto.point;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 포인트 지급 요청입니다. 금액 정책 검증은 서비스 계층에서 처리합니다.
 */
@Getter
@Setter
@NoArgsConstructor
public class PointEarnRequest {
    private int amount;
    private String content;
}
