package com.kb.youngly.dto.point;

import com.kb.youngly.enums.PointType;
import com.kb.youngly.vo.point.PointHistoryVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 포인트 지급 후 생성된 EARN 내역의 식별자와 지급 정보를 반환합니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointEarnResponse {
    private Long pointLogId;
    private PointType pointType;
    private Integer amount;
    private String content;

    /**
     * DB 저장 결과에서 지급 API에 필요한 필드만 응답 DTO로 변환합니다.
     */
    public static PointEarnResponse from(PointHistoryVO history) {
        return PointEarnResponse.builder()
                .pointLogId(history.getPointLogId())
                .pointType(history.getPointType())
                .amount(history.getAmount())
                .content(history.getContent())
                .build();
    }
}
