package com.kb.youngly.service;

import com.kb.youngly.dto.round.WeeklySettlementTarget;

import java.time.LocalDate;
import java.util.List;

/** 주간 챌린지 결산의 비즈니스 로직을 정의하는 서비스. */
public interface WeeklySettlementService {

    /**
     * 주어진 날짜에 주간 결산이 필요한 진행 중 라운드를 조회한다.
     *
     * @param settlementDate 한국 시간 기준 결산일
     * @return 결산할 라운드와 주차별 집계 기간 목록
     */
    List<WeeklySettlementTarget> findDueSettlements(LocalDate settlementDate);

    /**
     * 한 라운드의 한 주차를 결산한다.
     *
     * @param target 라운드, 그룹, 주차, 집계 기간 및 최소 인증 횟수 정보
     * @return 주간 목표를 달성하여 카운트가 증가한 참여자 수
     */
    int settle(WeeklySettlementTarget target);
}
