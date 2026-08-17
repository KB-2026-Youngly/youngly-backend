package com.kb.youngly.service;

import com.kb.youngly.dto.round.RoundSettlementParticipant;
import com.kb.youngly.dto.transfer.KbTransferCommand;
import com.kb.youngly.dto.transfer.KbTransferRequestResponse;
import com.kb.youngly.dto.transfer.KbTransferResult;
import com.kb.youngly.enums.GroupStatus;
import com.kb.youngly.enums.RoundStatus;
import com.kb.youngly.enums.TransactionCategory;
import com.kb.youngly.enums.TransactionType;
import com.kb.youngly.mapper.KbTransferMapper;
import com.kb.youngly.mapper.RoundMapper;
import com.kb.youngly.mapper.RoundSettlementMapper;
import com.kb.youngly.vo.account.AccountTransactionVO;
import com.kb.youngly.vo.group.GroupVO;
import com.kb.youngly.vo.round.RoundVO;
import com.kb.youngly.vo.transfer.KbTransferRequestVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** 실패가 확정된 라운드 정산 송금 한 건을 기존 요청 행으로 다시 실행한다. */
@Service
@RequiredArgsConstructor
public class KbTransferRetryService {

    /** 현재 Mock 계좌가 모두 국민은행 계좌이므로 거래 상대 은행명에 사용한다. */
    private static final String BANK_NAME = "국민";

    private final KbTransferRetryPreparationService retryPreparationService;
    private final KbTransferService kbTransferService;
    private final KbTransferMapper kbTransferMapper;
    private final RoundMapper roundMapper;
    private final RoundSettlementMapper roundSettlementMapper;

    /**
     * 그룹장이 소유한 FAILED 정산 요청을 재실행하고 갱신된 기존 요청을 반환한다.
     *
     * <p>새 {@code kb_transfer_requests} 행이나 새 멱등성 키를 만들지 않는다. 준비 서비스가
     * 기존 행을 PENDING으로 바꾼 다음, 최초 요청에 저장된 계좌·금액·멱등성 키를 그대로
     * 사용한다. 따라서 클라이언트가 재시도 금액이나 계좌를 변조할 수 없다.</p>
     *
     * <p>재송금 성공만 기록하고 끝내면 실제 KB 잔액은 바뀌었는데 {@code account_transactions},
     * 참여자 예치금 및 {@code round_history}는 실패 당시 상태로 남는다. 이 메서드는 재송금과
     * 해당 Youngly 후처리를 현재 트랜잭션에서 함께 처리하여 Mock 송금과 내부 원장이 원자적으로
     * 반영되게 한다. 마지막 미정산 참여자였다면 같은 잠금 안에서 라운드도 SETTLED로 전환한다.</p>
     */
    @Transactional
    public KbTransferRequestResponse retry(
            String userId,
            Long roundId,
            Long kbTransferRequestId
    ) {
        KbTransferRequestVO request = retryPreparationService.prepare(
                userId, roundId, kbTransferRequestId);

        /*
         * 그룹 -> 라운드 -> 참여자 순서로 최초 정산과 동일하게 잠근다. 같은 라운드의 실패 건을
         * 동시에 재시도하더라도 한 요청씩 처리되므로, 두 요청이 서로 상대방을 미정산으로 보고
         * 둘 다 라운드 완료 처리를 건너뛰는 경쟁 조건을 막는다.
         */
        RoundVO requestedRound = requireRound(request.getRoundId());
        GroupVO group = requireOngoingGroup(requestedRound.getGroupId());
        RoundVO waitingRound = requireWaitingRound(requestedRound);

        // KB 계좌는 아래 송금 서비스가 잠근다. 여기서는 업무 데이터인 미정산 이력만 먼저 잠근다.
        roundSettlementMapper.lockUnsettledParticipantsForUpdate(waitingRound.getRoundId());
        RoundSettlementParticipant participant = requireRetryParticipant(
                roundSettlementMapper.findParticipantsForUpdate(waitingRound.getRoundId()), request);

        KbTransferCommand command = KbTransferCommand.builder()
                .idempotencyKey(request.getIdempotencyKey())
                .sourceKbAccountId(request.getSourceKbAccountId())
                .destinationKbAccountId(request.getDestinationKbAccountId())
                .amount(request.getAmount())
                .transactionCategory(request.getTransactionCategory())
                .groupUserId(request.getGroupUserId())
                .settlementReceiverId(request.getSettlementReceiverId())
                .roundId(request.getRoundId())
                .build();

        // 성공 시 기존 송금 서비스의 markSuccess가 동일 PK 행을 SUCCESS로 갱신한다.
        KbTransferResult transferResult = kbTransferService.transfer(command);

        completeParticipantSettlement(
                participant, group, waitingRound, request, transferResult);

        /*
         * settlement_at이 없는 참여자가 남아 있으면 부분 정산 상태이므로 WAITING_SETTLEMENT를
         * 유지한다. 0명이면 현재 요청이 마지막 실패 건이었으므로 라운드를 SETTLED로 바꾼다.
         * markRoundSettled도 WAITING_SETTLEMENT와 종료일을 조건으로 사용해 잘못된 상태 덮어쓰기를
         * 한 번 더 방지한다.
         */
        if (roundSettlementMapper.countUnsettledParticipants(waitingRound.getRoundId()) == 0
                && roundSettlementMapper.markRoundSettled(
                waitingRound.getRoundId(), waitingRound.getEndDate()) != 1) {
            throw new IllegalStateException("모든 참여자 정산 후 라운드 완료 상태 변경에 실패했습니다.");
        }

        KbTransferRequestVO updated = kbTransferMapper.findById(kbTransferRequestId);
        if (updated == null) {
            throw new IllegalStateException("재시도한 정산 요청 결과를 찾을 수 없습니다.");
        }
        return toResponse(updated);
    }

    /** 요청의 round_id가 실제 라운드를 가리키는지 확인한다. */
    private RoundVO requireRound(Long roundId) {
        RoundVO round = roundMapper.findRoundById(roundId);
        if (round == null) {
            throw new IllegalStateException("재시도할 정산 라운드를 찾을 수 없습니다.");
        }
        return round;
    }

    /** 최초 정산과 같은 잠금 순서를 사용하고 정산 설정에 필요한 그룹 정보를 확보한다. */
    private GroupVO requireOngoingGroup(String groupId) {
        GroupVO group = roundMapper.findGroupForUpdate(groupId);
        if (group == null || group.getGroupStatus() != GroupStatus.ONGOING) {
            throw new IllegalStateException("진행 중인 정산 그룹을 찾을 수 없습니다.");
        }
        if (group.getBaseDepositAmount() == null || group.getBaseDepositAmount().signum() < 0) {
            throw new IllegalStateException("그룹의 최소 예치금이 올바르지 않습니다.");
        }
        return group;
    }

    /** 재시도는 아직 최종 완료되지 않은 동일 라운드에 대해서만 허용한다. */
    private RoundVO requireWaitingRound(RoundVO requestedRound) {
        if (requestedRound.getRoundStatus() != RoundStatus.WAITING_SETTLEMENT) {
            throw new IllegalStateException("정산 대기 상태의 라운드만 재시도할 수 있습니다.");
        }
        RoundVO locked = roundSettlementMapper.findWaitingRoundForUpdate(
                requestedRound.getGroupId(), requestedRound.getEndDate());
        if (locked == null || !requestedRound.getRoundId().equals(locked.getRoundId())) {
            throw new IllegalStateException("재시도할 정산 대기 라운드를 찾을 수 없습니다.");
        }
        return locked;
    }

    /**
     * 실패 요청의 수령자·계좌와 일치하는 미정산 참여자를 찾는다.
     * request.group_user_id는 수령자가 아니라 실패 목록을 관리하는 그룹장 ID이므로 비교하지 않는다.
     */
    private RoundSettlementParticipant requireRetryParticipant(
            List<RoundSettlementParticipant> participants,
            KbTransferRequestVO request) {
        return participants.stream()
                .filter(participant -> request.getSettlementReceiverId().equals(participant.getUserId()))
                .filter(participant -> request.getSourceKbAccountId().equals(
                        participant.getSourceKbAccountId()))
                .filter(participant -> request.getDestinationKbAccountId().equals(
                        participant.getDestinationKbAccountId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "실패 요청과 일치하는 미정산 참여자를 찾을 수 없습니다."));
    }

    /**
     * 재송금 성공 결과를 Youngly 업무 데이터에 반영한다.
     * 예치금, 양쪽 원장, round_history 중 하나라도 실패하면 retry의 @Transactional 경계가
     * Mock 계좌 잔액 변경과 SUCCESS 전환까지 함께 롤백하여 부분 완료 상태를 만들지 않는다.
     */
    private void completeParticipantSettlement(
            RoundSettlementParticipant participant,
            GroupVO group,
            RoundVO round,
            KbTransferRequestVO request,
            KbTransferResult transferResult) {
        BigDecimal amount = request.getAmount();
        if (participant.getCurrentDepositAmount() == null
                || participant.getCurrentDepositAmount().compareTo(amount) < 0) {
            throw new IllegalStateException("참여자의 현재 예치금이 재시도 정산액보다 적습니다.");
        }
        if (roundSettlementMapper.deductCurrentDepositAndUpdateStatus(
                participant.getGroupUserId(), amount, group.getBaseDepositAmount()) != 1) {
            throw new IllegalStateException("재시도 정산의 참여자 예치금 반영에 실패했습니다.");
        }

        String ledgerKeyPrefix = "settlement:" + round.getRoundId() + ":"
                + participant.getRoundHistoryId();
        insertTransaction(participant, round.getRoundId(), request.getSourceKbAccountId(),
                TransactionType.WITHDRAW, amount, transferResult.getSourceBalanceAfter(),
                ledgerKeyPrefix + ":moim", "라운드 미래 적립금 출금",
                participant.getDestinationAccountNumber(), participant.getDestinationAccountName());
        insertTransaction(participant, round.getRoundId(), request.getDestinationKbAccountId(),
                TransactionType.DEPOSIT, amount, transferResult.getDestinationBalanceAfter(),
                ledgerKeyPrefix + ":account", "라운드 미래 적립금 입금",
                participant.getSourceAccountNumber(), participant.getSourceAccountName());

        // settlement_at은 모든 금전·원장 처리가 성공한 뒤 마지막에 채워 완료 판정의 기준으로 쓴다.
        if (roundSettlementMapper.updateRoundHistorySettlement(
                participant.getRoundHistoryId(), amount) != 1) {
            throw new IllegalStateException("재시도 정산의 라운드 참여 이력 반영에 실패했습니다.");
        }
    }

    /** 실제 돈의 방향별로 출금·입금 원장을 각각 저장한다. */
    private void insertTransaction(
            RoundSettlementParticipant participant,
            Long roundId,
            String kbAccountId,
            TransactionType transactionType,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String idempotencyKey,
            String description,
            String anotherAccountNumber,
            String anotherName) {
        AccountTransactionVO transaction = new AccountTransactionVO();
        transaction.setKbAccountId(kbAccountId);
        transaction.setGroupUserId(participant.getGroupUserId());
        transaction.setRoundId(roundId);
        transaction.setTransactionType(transactionType);
        transaction.setTransactionCategory(TransactionCategory.SETTLEMENT);
        transaction.setAmount(amount);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setDescription(description);
        transaction.setAnotherAccountNumber(anotherAccountNumber);
        transaction.setAnotherBankName(BANK_NAME);
        transaction.setAnotherName(anotherName);
        if (roundSettlementMapper.insertAccountTransaction(transaction) != 1) {
            throw new IllegalStateException("재시도 정산 거래 원장 저장에 실패했습니다.");
        }
    }

    private KbTransferRequestResponse toResponse(KbTransferRequestVO request) {
        return KbTransferRequestResponse.builder()
                .transferRequestId(request.getKbTransferRequestId())
                .amount(request.getAmount())
                .transactionCategory(request.getTransactionCategory())
                .transferStatus(request.getTransferStatus())
                .kbTransactionId(request.getKbTransactionId())
                .failureCode(request.getFailureCode())
                .failureMessage(request.getFailureMessage())
                .settlementReceiverId(request.getSettlementReceiverId())
                .roundId(request.getRoundId())
                .requestedAt(request.getRequestedAt())
                .completedAt(request.getCompletedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}
