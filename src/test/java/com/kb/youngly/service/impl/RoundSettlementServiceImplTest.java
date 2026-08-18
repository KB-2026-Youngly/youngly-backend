package com.kb.youngly.service.impl;

import com.kb.youngly.dto.round.RoundSettlementParticipant;
import com.kb.youngly.dto.transfer.KbTransferCommand;
import com.kb.youngly.dto.transfer.KbTransferResult;
import com.kb.youngly.enums.GroupStatus;
import com.kb.youngly.enums.RoundStatus;
import com.kb.youngly.mapper.RoundMapper;
import com.kb.youngly.mapper.RoundSettlementMapper;
import com.kb.youngly.service.KbTransferAttemptService;
import com.kb.youngly.service.KbTransferRequestService;
import com.kb.youngly.vo.account.AccountTransactionVO;
import com.kb.youngly.vo.group.GroupVO;
import com.kb.youngly.vo.round.RoundVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoundSettlementServiceImplTest {

    private static final String GROUP_ID = "settlement-group";
    private static final String SOURCE_KB_ACCOUNT_ID = "kb-moim";
    private static final Long ROUND_ID = 100L;
    private static final LocalDate SETTLEMENT_DATE = LocalDate.of(2026, 8, 17);

    private RoundMapper roundMapper;
    private RoundSettlementMapper roundSettlementMapper;
    private KbTransferRequestService kbTransferRequestService;
    private KbTransferAttemptService kbTransferAttemptService;
    private RoundSettlementServiceImpl service;

    @BeforeEach
    void setUp() {
        roundMapper = mock(RoundMapper.class);
        roundSettlementMapper = mock(RoundSettlementMapper.class);
        kbTransferRequestService = mock(KbTransferRequestService.class);
        kbTransferAttemptService = mock(KbTransferAttemptService.class);
        service = new RoundSettlementServiceImpl(
                roundMapper,
                roundSettlementMapper,
                kbTransferRequestService,
                kbTransferAttemptService
        );
    }

    @Test
    @DisplayName("모든 독립 송금이 끝난 뒤 성공한 참여자의 계좌 원장을 저장한다")
    void settleRound_defersAccountTransactionsUntilAllTransfersFinish() {
        RoundSettlementParticipant first = participant(
                1001L, 2001L, "user01", 1, "kb-user01");
        RoundSettlementParticipant second = participant(
                1002L, 2002L, "user02", 2, "kb-user02");

        when(roundMapper.findGroupForUpdate(GROUP_ID)).thenReturn(group());
        when(roundSettlementMapper.findWaitingRoundForUpdate(
                GROUP_ID, SETTLEMENT_DATE.minusDays(1))).thenReturn(round());
        when(roundMapper.updateRoundRanks(ROUND_ID)).thenReturn(2);
        when(roundSettlementMapper.findParticipantsForUpdate(ROUND_ID))
                .thenReturn(List.of(first, second));
        when(roundSettlementMapper.findGroupLeaderGroupUserId(GROUP_ID, "user01"))
                .thenReturn(2001L);
        when(kbTransferAttemptService.transfer(any(KbTransferCommand.class)))
                .thenReturn(
                        transferResult(1L, "70000.00", "1030000.00"),
                        transferResult(2L, "20000.00", "1050000.00")
                );
        when(roundSettlementMapper.deductCurrentDepositAndUpdateStatus(
                any(Long.class), any(BigDecimal.class), any(BigDecimal.class))).thenReturn(1);
        when(roundSettlementMapper.updateRoundHistorySettlement(
                any(Long.class), any(BigDecimal.class))).thenReturn(1);
        when(roundSettlementMapper.insertAccountTransaction(any(AccountTransactionVO.class)))
                .thenReturn(1);
        when(roundSettlementMapper.markRoundSettled(
                ROUND_ID, SETTLEMENT_DATE.minusDays(1))).thenReturn(1);

        assertTrue(service.settleRound(GROUP_ID, SETTLEMENT_DATE));

        InOrder order = inOrder(kbTransferAttemptService, roundSettlementMapper);
        order.verify(kbTransferAttemptService).transfer(commandFor("user01"));
        order.verify(roundSettlementMapper).deductCurrentDepositAndUpdateStatus(
                2001L, new BigDecimal("30000.00"), new BigDecimal("100000.00"));
        order.verify(roundSettlementMapper).updateRoundHistorySettlement(
                1001L, new BigDecimal("30000.00"));
        order.verify(kbTransferAttemptService).transfer(commandFor("user02"));
        order.verify(roundSettlementMapper).deductCurrentDepositAndUpdateStatus(
                2002L, new BigDecimal("50000.00"), new BigDecimal("100000.00"));
        order.verify(roundSettlementMapper).updateRoundHistorySettlement(
                1002L, new BigDecimal("50000.00"));
        order.verify(roundSettlementMapper, times(4))
                .insertAccountTransaction(any(AccountTransactionVO.class));
        verify(roundSettlementMapper).markRoundSettled(
                ROUND_ID, SETTLEMENT_DATE.minusDays(1));
    }

    private GroupVO group() {
        return GroupVO.builder()
                .groupId(GROUP_ID)
                .userId("user01")
                .baseDepositAmount(new BigDecimal("100000.00"))
                .futureDepositRatioRule("1:30/2:50")
                .groupStatus(GroupStatus.ONGOING)
                .build();
    }

    private RoundVO round() {
        return RoundVO.builder()
                .roundId(ROUND_ID)
                .groupId(GROUP_ID)
                .roundNo(1)
                .startDate(LocalDate.of(2026, 7, 20))
                .endDate(SETTLEMENT_DATE.minusDays(1))
                .roundStatus(RoundStatus.WAITING_SETTLEMENT)
                .build();
    }

    private RoundSettlementParticipant participant(
            Long roundHistoryId, Long groupUserId, String userId,
            int rankNo, String destinationKbAccountId) {
        return RoundSettlementParticipant.builder()
                .roundHistoryId(roundHistoryId)
                .groupUserId(groupUserId)
                .userId(userId)
                .rankNo(rankNo)
                .currentDepositAmount(new BigDecimal("100000.00"))
                .destinationKbAccountId(destinationKbAccountId)
                .destinationAccountNumber("025202-00-" + groupUserId)
                .destinationAccountName(userId)
                .sourceKbAccountId(SOURCE_KB_ACCOUNT_ID)
                .sourceAccountNumber("025202-22-000001")
                .sourceAccountName("group-owner")
                .build();
    }

    private KbTransferResult transferResult(
            Long requestId, String sourceBalanceAfter, String destinationBalanceAfter) {
        return new KbTransferResult(
                requestId,
                "MOCK-KB-" + requestId,
                new BigDecimal(sourceBalanceAfter),
                new BigDecimal(destinationBalanceAfter)
        );
    }

    private KbTransferCommand commandFor(String userId) {
        return argThat(command -> userId.equals(command.getSettlementReceiverId()));
    }
}
