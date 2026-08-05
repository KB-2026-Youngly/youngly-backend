package com.kb.youngly.dto.round;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 한 주차의 챌린지 결산을 실행하는 데 필요한 조건을 전달하는 DTO.
 *
 * <p>매일 스케줄러가 실행되면 Mapper가 결산일에 해당하는 진행 중 라운드를
 * 조회하고, 조회 결과를 이 객체로 변환한다. 스케줄러는 이 객체를 서비스에
 * 전달하고, 서비스는 다시 Mapper에 전달하여 다음 작업에 사용한다.</p>
 *
 * <ul>
 *     <li>해당 주차의 사용자별 승인 게시물 수 집계 및 저장</li>
 *     <li>최소 인증 횟수를 달성한 사용자의 success와 streak 갱신</li>
 * </ul>
 *
 * <p>이 DTO는 {@code weekly_settlements} 테이블의 한 행을 표현하는 객체가 아니다.
 * 테이블에 저장된 사용자별 결산 결과는 {@code WeeklySettlementVO}가 표현하며,
 * 이 객체는 하나의 라운드와 주차를 결산하기 위한 실행 조건만 담는다.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklySettlementTarget {

    /** 결산할 라운드의 ID. */
    private Long roundId;

    /** streak를 갱신할 group_users 행의 그룹 ID. */
    private String groupId;

    /** 라운드 시작일을 기준으로 계산된 결산 주차 번호. */
    private Integer weekNo;

    /** 승인 게시물을 집계할 주간 범위의 시작일. */
    private LocalDate weekStartDate;

    /** 승인 게시물을 집계할 주간 범위의 종료일. */
    private LocalDate weekEndDate;

    /** 해당 주차를 성공으로 인정하기 위해 필요한 최소 승인 게시물 수. */
    private Integer minCount;
}
