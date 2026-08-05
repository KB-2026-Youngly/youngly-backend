package com.kb.youngly.dto.round;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/** 개발용 수동 라운드 전환 API의 실행 결과. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoundTransitionExecutionResponse {

    /** 라운드 종료일과 비교한 전환 기준일. */
    private LocalDate transitionDate;

    /** 조회 시점에 자동 전환 조건을 만족한 그룹 수. */
    private Integer targetCount;

    /** 종료 상태 변경과 다음 라운드 생성이 완료된 그룹 수. */
    private Integer transitionedCount;

    /** 중복 실행 등으로 처리 시점에 전환할 대상이 없어 건너뛴 그룹 수. */
    private Integer skippedCount;

    /** 예외가 발생하여 트랜잭션이 롤백된 그룹 수. */
    private Integer failedCount;

    /** 그룹별 실패 식별자와 예외 메시지. */
    private List<String> failures;
}
