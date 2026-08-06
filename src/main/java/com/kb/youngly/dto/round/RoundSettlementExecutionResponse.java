package com.kb.youngly.dto.round;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/** 개발용 수동 라운드 정산 API의 실행 결과. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoundSettlementExecutionResponse {

    /** 라운드 종료일의 다음 날과 비교하는 정산 기준일. */
    private LocalDate settlementDate;

    /** 조회 시점에 자동 정산 조건을 만족한 그룹 수. */
    private Integer targetCount;

    /** 계좌 이체와 라운드 완료 처리가 모두 성공한 그룹 수. */
    private Integer settledCount;

    /** 중복 실행 등으로 처리 시점에 대상이 사라져 건너뛴 그룹 수. */
    private Integer skippedCount;

    /** 예외가 발생하여 그룹 단위 트랜잭션이 롤백된 그룹 수. */
    private Integer failedCount;

    /** 실패한 그룹 ID와 예외 메시지 목록. */
    private List<String> failures;
}
