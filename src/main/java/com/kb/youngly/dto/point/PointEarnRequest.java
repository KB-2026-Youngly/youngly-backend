package com.kb.youngly.dto.point;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 지급할 포인트와 내역에 남길 내용을 전달합니다.
 * 양수 및 10포인트 단위 검증은 서비스 계층에서 처리합니다.
 */
@Getter
@Setter
@NoArgsConstructor
public class PointEarnRequest {
    private int amount;
    private String content;
}
