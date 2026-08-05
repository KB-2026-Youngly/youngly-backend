package com.kb.youngly.vo.round;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자별 주간 챌린지 결산 결과.
 *
 * <p>{@code weekly_settlements} 테이블의 한 행과 대응하며, 특정 사용자가
 * 한 라운드의 한 주차 동안 승인받은 게시물 수를 보관한다.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklySettlementVO {

    /** 주간 결산 결과의 자동 증가 기본키. */
    private Long weeklySettlementId;

    /** 결산 대상 라운드 ID. */
    private Long roundId;

    /** 라운드 시작일을 기준으로 계산한 주차 번호. */
    private Integer weekNo;

    /** 결산 대상 사용자 ID. */
    private String userId;

    /** 해당 주차 동안 최종 승인된 사용자의 게시물 수. */
    private Integer approvedPostCount;

    /** 주간 결산 결과가 생성된 시각. */
    private LocalDateTime createdAt;
}
