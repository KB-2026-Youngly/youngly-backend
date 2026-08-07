package com.kb.youngly.mapper;

import com.kb.youngly.dto.round.WeeklySettlementTarget;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/** 주간 챌린지 결산 조회 및 갱신 Mapper. */
public interface WeeklySettlementMapper {

    /** 지정한 날짜가 시작일로부터 7, 14, 21...일째인 진행 중 라운드를 조회한다. */
    List<WeeklySettlementTarget> findDueWeeklySettlements(
            @Param("settlementDate") LocalDate settlementDate);

    /** 참여자별 승인 게시물 수를 주간 결산 결과로 저장한다. */
    int insertWeeklySettlementResults(WeeklySettlementTarget target);

    /** 목표를 달성했거나 실패 패스로 부족분을 충당한 참여자의 성공 횟수를 증가시킨다. */
    int incrementRoundHistorySuccessCount(WeeklySettlementTarget target);

    /** 목표 달성 또는 실패 패스 사용으로 성공한 참여자의 그룹 연속 성공 횟수를 증가시킨다. */
    int incrementGroupUserStreakCount(WeeklySettlementTarget target);
}
