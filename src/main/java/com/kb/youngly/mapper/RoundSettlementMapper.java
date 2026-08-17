package com.kb.youngly.mapper;

import com.kb.youngly.dto.round.RoundSettlementParticipant;
import com.kb.youngly.vo.account.AccountTransactionVO;
import com.kb.youngly.vo.round.RoundVO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 라운드 정산 대상, 참여자 예치금, 정산 이력 및 거래원장을 담당하는 Mapper.
 * KB 계좌 잠금과 잔액 변경은 송금 경계를 분명히 하기 위해 {@link KbTransferMapper}로 분리한다.
 */
public interface RoundSettlementMapper {

    List<String> findDueSettlementGroupIds(@Param("settlementDate") LocalDate settlementDate);

    RoundVO findWaitingRoundForUpdate(@Param("groupId") String groupId,
                                      @Param("endDate") LocalDate endDate);

    /** 미정산 참여자의 round_history와 group_users 행만 잠그고 KB 계좌는 잠그지 않는다. */
    List<Long> lockUnsettledParticipantsForUpdate(@Param("roundId") Long roundId);

    List<RoundSettlementParticipant> findParticipantsForUpdate(@Param("roundId") Long roundId);

    /** 새 라운드 준비를 위해 기존 실패 대응 선택을 전체 참여자에서 제거한다. */
    int clearPriorFailureResponses(@Param("roundId") Long roundId);

    /** 정산 실패 목록의 소유자로 기록할 그룹장의 group_user_id를 조회한다. */
    Long findGroupLeaderGroupUserId(@Param("groupId") String groupId,
                                    @Param("leaderUserId") String leaderUserId);

    int deductCurrentDepositAndUpdateStatus(@Param("groupUserId") Long groupUserId,
                                            @Param("amount") BigDecimal amount,
                                            @Param("baseDepositAmount") BigDecimal baseDepositAmount);

    int updateRoundHistorySettlement(@Param("roundHistoryId") Long roundHistoryId,
                                     @Param("settlementAmount") BigDecimal settlementAmount);

    int insertAccountTransaction(AccountTransactionVO transaction);

    /** 재시도 후 아직 정산 완료 시각이 없는 라운드 참여자 수를 조회한다. */
    int countUnsettledParticipants(@Param("roundId") Long roundId);

    int markRoundSettled(@Param("roundId") Long roundId,
                         @Param("endDate") LocalDate endDate);
}
