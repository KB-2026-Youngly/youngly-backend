package com.kb.youngly.mapper;

import com.kb.youngly.dto.round.*;
import com.kb.youngly.vo.group.GroupVO;
import com.kb.youngly.vo.round.RoundHistoryVO;
import com.kb.youngly.vo.round.RoundVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/** 라운드와 라운드 참여 이력 생성에 필요한 MyBatis Mapper. */
public interface RoundMapper {

    /** 지정한 날짜에 종료되는 진행 중 그룹의 진행 중 라운드 그룹 ID를 조회한다. */
    List<String> findDueRoundGroupIds(@Param("endDate") LocalDate endDate);

    /** 다음 회차 번호가 중복되지 않도록 그룹 행을 잠근다. */
    GroupVO findGroupForUpdate(String groupId);

    /** 회차 번호가 가장 큰 직전 라운드를 조회한다. 첫 라운드라면 null을 반환한다. */
    RoundVO findLatestRound(String groupId);

    /** 그룹 잠금 이후 기준일에 종료되는 진행 중 라운드를 다시 조회한다. */
    RoundVO findOngoingRoundEndingOn(@Param("groupId") String groupId,
                                     @Param("endDate") LocalDate endDate);

    /** 라운드를 저장하고 자동 생성된 roundId를 전달받는다. */
    int insertRound(RoundVO round);

    /** 지정한 진행 중 라운드를 정산 대기 상태로 변경한다. */
    int markRoundWaitingSettlement(@Param("roundId") Long roundId,
                                   @Param("endDate") LocalDate endDate);

    /** 모집 중인 그룹의 상태를 진행 중으로 변경한다. */
    int startRecruitingGroup(String groupId);

    /**
     * PENDING_DEPOSIT 또는 ACTIVE 상태인 사용자와
     * 각 사용자의 미래 적립금 수령 계좌를 조회한다.
     */
    List<RoundParticipantDTO> findRoundParticipants(String groupId);

    /** 한 사용자의 라운드 시작 시점 이력을 저장한다. */
    int insertRoundHistory(RoundHistoryVO roundHistory);

    /**
     * 라운드 조회
     */
    RoundVO findRoundById(Long roundId);

    /**
     * 라운드 참여자 조회
     */
    // 누적 성공 횟수를 기준으로 라운드 순위 저장
    int updateRoundRanks(@Param("roundId") Long roundId);
    // 라운드 참여자 조회
    List<RoundUserResponse> findRoundUsers(@Param("roundId") Long roundId);
    // 라운드별 사용자 랭킹 조회
    List<RoundRankingResponse> findRoundRanking(@Param("roundId") Long roundId);
    // 라운드별 정산 결과 조회
    List<RoundSettlementResponse> findRoundSettlements(@Param("roundId") Long roundId);
    // 라운드 존재 여부 확인
    boolean existsRound(@Param("roundId") Long roundId);

}
