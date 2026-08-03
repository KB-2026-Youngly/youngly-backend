package com.kb.youngly.dto.point;

import com.kb.youngly.enums.PointType;
import com.kb.youngly.vo.point.PointHistoryVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 포인트 적립·사용 내역 한 건에서 클라이언트에 공개할 정보를 반환합니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointHistoryResponse {
    private Long pointLogId;
    private Long itemId;
    private PointType pointType;
    private Integer amount;
    private String content;
    private LocalDateTime createdAt;

    /**
     * 내부 VO에서 사용자 ID를 제외한 응답 필드만 복사합니다.
     */
    public static PointHistoryResponse from(PointHistoryVO history) {
        return PointHistoryResponse.builder()
                .pointLogId(history.getPointLogId())
                .itemId(history.getItemId())
                .pointType(history.getPointType())
                .amount(history.getAmount())
                .content(history.getContent())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
