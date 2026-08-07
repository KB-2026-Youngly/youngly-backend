package com.kb.youngly.mapper;

import com.kb.youngly.dto.round.RoundSettlementParticipant;
import com.kb.youngly.vo.account.AccountTransactionVO;
import com.kb.youngly.vo.round.RoundVO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 라운드 정산에 필요한 잠금 조회, 잔액 변경, 원장 저장 MyBatis Mapper. */
public interface RoundSettlementMapper {

    List<String> findDueSettlementGroupIds(@Param("settlementDate") LocalDate settlementDate);

    RoundVO findWaitingRoundForUpdate(@Param("groupId") String groupId,
                                      @Param("endDate") LocalDate endDate);

    List<RoundSettlementParticipant> findParticipantsForUpdate(@Param("roundId") Long roundId);

    BigDecimal findKbAccountBalanceForUpdate(@Param("kbAccountId") String kbAccountId);

    int withdrawFromKbAccount(@Param("kbAccountId") String kbAccountId,
                              @Param("amount") BigDecimal amount);

    int increaseKbAccountBalance(@Param("kbAccountId") String kbAccountId,
                                 @Param("amount") BigDecimal amount);

    int deductCurrentDepositAndUpdateStatus(@Param("groupUserId") Long groupUserId,
                                            @Param("amount") BigDecimal amount,
                                            @Param("baseDepositAmount") BigDecimal baseDepositAmount);

    int updateRoundHistorySettlement(@Param("roundHistoryId") Long roundHistoryId,
                                     @Param("settlementAmount") BigDecimal settlementAmount);

    int insertAccountTransaction(AccountTransactionVO transaction);

    int markRoundSettled(@Param("roundId") Long roundId,
                         @Param("endDate") LocalDate endDate);
}
