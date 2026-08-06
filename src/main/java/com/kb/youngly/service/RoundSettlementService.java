package com.kb.youngly.service;

import java.time.LocalDate;
import java.util.List;

/** 종료 다음 날이 된 정산 대기 라운드의 계좌 이체와 상태 변경을 담당한다. */
public interface RoundSettlementService {

    /** 지정일이 종료일 다음 날인 정산 대상 진행 그룹 ID를 조회한다. */
    List<String> findDueGroupIds(LocalDate settlementDate);

    /** 한 그룹의 대상 라운드를 단일 트랜잭션으로 정산한다. */
    boolean settleRound(String groupId, LocalDate settlementDate);
}
