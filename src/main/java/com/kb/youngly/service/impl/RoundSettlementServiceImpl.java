package com.kb.youngly.service.impl;

import com.kb.youngly.dto.round.RoundSettlementParticipant;
import com.kb.youngly.dto.transfer.KbTransferCommand;
import com.kb.youngly.dto.transfer.KbTransferResult;
import com.kb.youngly.enums.GroupStatus;
import com.kb.youngly.enums.TransactionCategory;
import com.kb.youngly.enums.TransactionType;
import com.kb.youngly.exception.RoundSettlementIncompleteException;
import com.kb.youngly.mapper.RoundMapper;
import com.kb.youngly.mapper.RoundSettlementMapper;
import com.kb.youngly.service.KbTransferAttemptService;
import com.kb.youngly.service.KbTransferRequestService;
import com.kb.youngly.service.RoundSettlementService;
import com.kb.youngly.vo.account.AccountTransactionVO;
import com.kb.youngly.vo.group.GroupVO;
import com.kb.youngly.vo.round.RoundVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 순위별 미래 적립금을 모임통장에서 참여자 계좌로 이체하는 서비스.
 *
 * <p>그룹의 정산 이력과 원장은 그룹 트랜잭션에서 관리하고, 실제 KB 송금은 참여자별
 * 독립 트랜잭션에서 실행한다. 한 참여자의 송금이 실패해도 다음 참여자를 계속 처리하며,
 * 모든 참여자가 성공한 경우에만 라운드를 최종 정산 완료 상태로 바꾼다.</p>
 */
@Service
@RequiredArgsConstructor
public class RoundSettlementServiceImpl implements RoundSettlementService {

    /** 비율 규칙의 퍼센트 값을 실제 금액으로 환산할 때 사용하는 100. */
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    /** DECIMAL(19,2) 금액 컬럼과 동일하게 정산 금액을 소수 둘째 자리까지 저장한다. */
    private static final int MONEY_SCALE = 2;
    /** 현재 Mock 계좌가 모두 국민은행 계좌이므로 거래 상대 은행명에 사용한다. */
    private static final String BANK_NAME = "국민";

    /** 그룹 행 잠금과 그룹 설정 조회에는 기존 라운드 Mapper를 재사용한다. */
    private final RoundMapper roundMapper;
    /** 정산 대상 조회, 참여자 예치금, 라운드 이력 및 거래원장 변경을 담당한다. */
    private final RoundSettlementMapper roundSettlementMapper;
    /** 각 참여자 송금 전에 PENDING 요청을 독립 트랜잭션으로 먼저 확정한다. */
    private final KbTransferRequestService kbTransferRequestService;
    /** 한 참여자의 송금 실패가 다른 참여자의 시도를 중단하지 않도록 독립 실행한다. */
    private final KbTransferAttemptService kbTransferAttemptService;


    /**
     * 스케줄 실행일을 기준으로 정산해야 할 그룹을 조회한다.
     *
     * <p>Mapper에서는 진행 중인 그룹 중 라운드가 정산 대기 상태이고,
     * {@code end_date = settlementDate - 1일}인 그룹만 반환한다. 이 단계는
     * 대상 후보 조회일 뿐이며 실제 정산 시 잠금 안에서 조건을 다시 확인한다.</p>
     *
     * @param settlementDate 한국시간 기준 스케줄 실행일
     * @return 정산 대상이 존재하는 그룹 ID 목록
     */
    @Override
    @Transactional(readOnly = true)
    public List<String> findDueGroupIds(LocalDate settlementDate) {
        // null 날짜가 SQL의 DATE_SUB에 전달되어 의도치 않은 빈 결과가 나오지 않게 한다.
        if (settlementDate == null) {
            throw new IllegalArgumentException("라운드 정산 기준일은 필수입니다.");
        }
        return roundSettlementMapper.findDueSettlementGroupIds(settlementDate);
    }

    /**
     * 한 그룹의 정산 대상 라운드를 끝까지 처리한다.
     *
     * <p>{@code @Transactional} 경계 안에서 그룹 및 대상 데이터를 잠그고 다음 작업을
     * 순서대로 수행한다.</p>
     *
     * <ol>
     *     <li>순위별 비율 규칙 파싱 및 참여자 정산 금액 계산</li>
     *     <li>모임통장 출금과 참여자 수령 계좌 입금</li>
     *     <li>개인별 현재 예치금 차감 및 필요 시 재충전 대기 상태 전환</li>
     *     <li>출금·입금 거래 원장과 라운드 참여 이력 저장</li>
     *     <li>모든 참여자가 성공한 경우 라운드를 SETTLED로 변경</li>
     * </ol>
     *
     * <p>개별 송금 실패는 모아서 마지막에 {@link RoundSettlementIncompleteException}으로
     * 보고한다. 이 예외는 그룹 트랜잭션의 롤백 대상에서 제외되므로 성공한 참여자의 결과와
     * 전체 참여자의 {@code prior_failure_response = NULL} 변경은 유지된다.</p>
     *
     * @param groupId 정산 대상 라운드가 속한 그룹 ID
     * @param settlementDate 한국시간 기준 정산 실행일
     * @return 정산을 완료했으면 true, 이미 처리됐거나 대상이 아니면 false
     */
    @Override
    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            noRollbackFor = RoundSettlementIncompleteException.class
    )
    public boolean settleRound(String groupId, LocalDate settlementDate) {
        // 잠금 쿼리 실행 전에 필수 식별자와 날짜를 검증한다.
        if (groupId == null || groupId.trim().isEmpty() || settlementDate == null) {
            throw new IllegalArgumentException("라운드 정산 대상 정보가 올바르지 않습니다.");
        }

        // 앞뒤 공백 때문에 동일 그룹이 서로 다른 값처럼 처리되지 않도록 정규화한다.
        String normalizedGroupId = groupId.trim();

        // 동일 그룹의 라운드 전환·정산이 겹치지 않게 그룹 행을 먼저 잠근다.
        // 이 잠금은 SELECT 실행 순간만이 아니라 현재 트랜잭션의 커밋/롤백까지 유지된다.
        GroupVO group = roundMapper.findGroupForUpdate(normalizedGroupId);
        // 대상 조회 후 그룹이 삭제되거나 종료됐다면 아무 데이터도 변경하지 않는다.
        if (group == null || group.getGroupStatus() != GroupStatus.ONGOING) {
            return false;
        }
        validateGroupSettings(group);

        // 실행일이 종료일의 다음 날이어야 하므로 실제 조회 조건에는 전날을 사용한다.
        LocalDate expectedEndDate = settlementDate.minusDays(1);
        // 스케줄 대상 조회 뒤 상태가 바뀌었을 수 있으므로 잠금 안에서 다시 확인한다.
        // 라운드 행도 FOR UPDATE로 잠겨 정산 완료 전 다른 작업이 상태를 바꾸지 못한다.
        RoundVO round = roundSettlementMapper.findWaitingRoundForUpdate(
                normalizedGroupId, expectedEndDate);
        if (round == null) {
            return false;
        }

        // 예: "1:30/2:50/3:70"을 {1=30, 2=50, 3=70} 형태로 변환한다.
        Map<Integer, BigDecimal> ratiosByRank = parseRatioRule(group.getFutureDepositRatioRule());
        // round_history, group_users 및 연결 계좌 정보를 조회하며 변경 대상 행을 잠근다.
        // 누적된 주간 성공 횟수를 기준으로 참여자별 최종 순위를 저장한다.
        int updatedRankCount =
                roundMapper.updateRoundRanks(round.getRoundId());

        if (updatedRankCount == 0) {
            throw new IllegalStateException(
                    "순위를 계산할 라운드 참여자가 없습니다."
            );
        }

        /*
         * prior_failure_response는 송금 성공자만이 아니라 이번 정산 대상 전체에서 비운다.
         * 아래 개별 송금 중 일부가 실패하더라도 불완전 정산 예외는 롤백 대상이 아니므로
         * 이 초기화가 커밋되어 이전 라운드의 대응 선택이 다음 라운드로 넘어가지 않는다.
         */
        roundSettlementMapper.clearPriorFailureResponses(round.getRoundId());

        /*
         * 참여자 업무 행은 바깥 트랜잭션에서 보호하되 KB 계좌는 잠그지 않는다.
         * KB 계좌는 참가자별 REQUIRES_NEW 송금 트랜잭션에서 잠가야 바깥 트랜잭션과
         * 안쪽 트랜잭션이 서로 같은 계좌 잠금을 기다리는 교착 상태를 피할 수 있다.
         */
        roundSettlementMapper.lockUnsettledParticipantsForUpdate(round.getRoundId());

        List<RoundSettlementParticipant> participants =
                roundSettlementMapper.findParticipantsForUpdate(round.getRoundId());
        if (participants.isEmpty()) {
            throw new IllegalStateException("정산할 라운드 참여 이력이 없습니다.");
        }

        /*
         * 정산 송금 실패 목록은 실제 수령자가 아니라 그룹장이 관리할 예정이다.
         * groups.user_id는 사용자 식별자이고 kb_transfer_requests.group_user_id는
         * group_users의 기본키를 참조하므로, 그룹장의 그룹 참여 ID를 한 번 조회해
         * 현재 라운드의 모든 참여자 송금 요청에 공통으로 저장한다.
         */
        Long leaderGroupUserId = requireGroupLeaderGroupUserId(
                normalizedGroupId, group.getUserId());

        // 모든 참여자가 같은 모임통장을 사용하는지 먼저 확인한다.
        // 실제 잔액 조회·잠금·부족 여부 검증은 KbTransferService가 이체 건별로 담당한다.
        String sourceKbAccountId = requireCommonSourceAccount(participants);

        // 한 참여자의 실패로 반복문을 종료하지 않고 모든 실패를 모아 마지막에 보고한다.
        List<String> transferFailures = new ArrayList<>();
        /*
         * account_transactions.kb_account_id는 kb_accounts를 참조하는 FK다. 바깥 정산
         * 트랜잭션에서 첫 성공자의 원장을 즉시 저장하면 공통 모임통장 행에
         * FK 공유 잠금이 유지된다. 다음 참여자의 REQUIRES_NEW 송금이 같은 행을
         * FOR UPDATE로 잠그려면 바깥 트랜잭션이 끝나기를 기다리게 되므로, 모든
         * 독립 송금 시도가 끝난 뒤에만 원장을 저장한다.
         */
        List<DeferredSettlementLedger> deferredLedgers = new ArrayList<>();

        for (RoundSettlementParticipant participant : participants) {
            // 순위, 참여자, 출발·도착 계좌가 모두 갖춰진 경우에만 금액을 계산한다.
            validateParticipant(participant);
            // 공동 순위는 rank_no가 같으므로 같은 비율과 같은 정산 금액이 각각 적용된다.
            // 비율 규칙에 없는 순위는 calculateSettlementAmount에서 0.00원으로 처리한다.
            BigDecimal settlementAmount = calculateSettlementAmount(
                    group.getBaseDepositAmount(), ratiosByRank.get(participant.getRankNo()));

            // 0원 정산은 실제 계좌 잔액과 거래 원장을 변경하지 않고 이력만 확정한다.
            if (settlementAmount.signum() > 0) {
                // 개인별 예치금보다 많은 금액을 지급하지 않으며, 불일치 시 그룹 전체를 롤백한다.
                if (participant.getCurrentDepositAmount().compareTo(settlementAmount) < 0) {
                    throw new IllegalStateException(
                            participant.getUserId() + " 사용자의 현재 예치금이 정산액보다 적습니다.");
                }
                /*
                 * 참여자와 라운드를 조합한 키는 이 정산 건에서 항상 같은 값을 만든다.
                 * 배치가 재실행돼도 KbTransferService가 기존 SUCCESS 요청을 찾아 실제 계좌
                 * 잔액을 다시 변경하지 않으므로 동일 정산의 중복 송금을 방지할 수 있다.
                 *
                 * 요청 서비스는 PENDING을 먼저 커밋하고, 송금 서비스는 출금·입금 계좌 잠금,
                 * 잔액 부족 검사, 양쪽 잔액 변경 및 SUCCESS 전환만 담당한다. 실패 시 예외가
                 * 발생하므로 아래 후속 로직은 성공이 확인된 경우에만 실행된다.
                 */
                String transferIdempotencyKey = "settlement:"
                        + round.getRoundId() + ":" + participant.getRoundHistoryId();
                KbTransferCommand transferCommand = KbTransferCommand.builder()
                        .idempotencyKey(transferIdempotencyKey)
                        .sourceKbAccountId(sourceKbAccountId)
                        .destinationKbAccountId(
                                participant.getDestinationKbAccountId())
                        .amount(settlementAmount)
                        .transactionCategory(TransactionCategory.SETTLEMENT)
                        // 정산 실패 내역을 조회하고 관리할 주체는 그룹장이다.
                        .groupUserId(leaderGroupUserId)
                        // 실제 돈을 받는 대상은 round_history.user_id에 해당하는 참여자다.
                        .settlementReceiverId(participant.getUserId())
                        .roundId(round.getRoundId())
                        .build();

                /*
                 * 실제 송금보다 먼저 PENDING을 별도 트랜잭션으로 커밋한다. 따라서 외부 KB가
                 * 요청을 처리한 직후 애플리케이션이 중단되더라도 송금 의도와 멱등성 키가 남는다.
                 * 재실행 시 같은 키의 기존 요청을 사용하므로 새 송금 요청 행도 만들지 않는다.
                 */
                KbTransferResult transferResult;
                try {
                    kbTransferRequestService.createPending(transferCommand);

                    /*
                     * 참가자 한 명의 송금을 별도 트랜잭션에서 실행한다. 실패한 트랜잭션은
                     * 해당 요청만 롤백한 뒤 FAILED를 기록하며, 여기서는 예외를 수집하고
                     * continue하여 다음 round_history 참여자의 송금을 계속 시도한다.
                     */
                    transferResult = kbTransferAttemptService.transfer(transferCommand);
                } catch (RuntimeException exception) {
                    /*
                     * 이 catch는 PENDING 생성 또는 실제 송금 실패만 참가자별 실패로 수집한다.
                     * 예치금·원장·round_history 반영까지 같은 catch에 넣으면, 첫 번째 원장 저장 후
                     * 두 번째 원장이 실패해도 마지막 RoundSettlementIncompleteException이 롤백에서
                     * 제외되어 일부 업무 데이터만 커밋될 수 있다.
                     */
                    transferFailures.add("userId=" + participant.getUserId()
                            + ", message=" + exception.getMessage());
                    continue;
                }

                /*
                 * 송금 성공 뒤의 Youngly 내부 후처리는 참여자별 실패로 삼지 않고 바깥 정산
                 * 트랜잭션의 필수 작업으로 처리한다. 아래 작업 중 하나라도 실패하면 일반 예외가
                 * 전파되어 이번 정산의 예치금·원장·이력 변경이 모두 롤백된다. 이미 독립 트랜잭션으로
                 * 성공한 송금 요청은 멱등성 키와 잔액 결과가 남으므로 다음 배치에서 안전하게
                 * 재사용하여 내부 후처리를 다시 수행할 수 있다.
                 */
                if (roundSettlementMapper.deductCurrentDepositAndUpdateStatus(
                        participant.getGroupUserId(), settlementAmount,
                        group.getBaseDepositAmount()) != 1) {
                    throw new IllegalStateException("참여자 예치금 반영에 실패했습니다.");
                }

                // 원장에는 송금이 실제 변경에 사용한 잔액 결과를 나중에 기록한다.
                deferredLedgers.add(new DeferredSettlementLedger(
                        participant, settlementAmount, transferResult));
            }

            // 규칙에 없는 순위도 0원으로 확정하고 이전 실패 대응값을 비운다.
            if (roundSettlementMapper.updateRoundHistorySettlement(
                    participant.getRoundHistoryId(), settlementAmount) != 1) {
                throw new IllegalStateException("라운드 참여 이력 정산 반영에 실패했습니다.");
            }
        }

        /*
         * 모든 REQUIRES_NEW 송금이 커밋 또는 롤백된 뒤이므로, 이제 FK 공유 잠금이
         * 생겨도 뒤에 계좌 행을 다시 잠글 독립 송금이 없다. 부분 송금 실패가 있어도
         * 성공자의 원장은 noRollbackFor 정책에 따라 예치금·정산 이력과 함께 커밋된다.
         */
        for (DeferredSettlementLedger deferredLedger : deferredLedgers) {
            insertDeferredSettlementLedger(
                    deferredLedger, round.getRoundId(), sourceKbAccountId);
        }

        /*
         * 실패자가 한 명이라도 있으면 라운드는 WAITING_SETTLEMENT로 유지한다.
         * 단, 예외를 던지기 전 모든 참가자를 이미 시도했으며 noRollbackFor 설정에 의해
         * 성공 결과와 prior_failure_response 초기화는 커밋된다.
         */
        if (!transferFailures.isEmpty()) {
            throw new RoundSettlementIncompleteException(
                    "일부 참여자 정산 송금에 실패했습니다: " + String.join(" | ", transferFailures));
        }

        // 모든 참여자의 이체와 원장 저장이 성공한 뒤에만 최종 정산 완료 상태로 전환한다.
        if (roundSettlementMapper.markRoundSettled(round.getRoundId(), expectedEndDate) != 1) {
            throw new IllegalStateException("라운드 정산 완료 상태 변경에 실패했습니다.");
        }
        return true;
    }

    /**
     * {@code "1:30/2:50"} 형식을 순위-퍼센트 맵으로 엄격하게 변환한다.
     *
     * <p>순위는 1 이상의 정수, 비율은 0 이상 100 이하만 허용한다. 동일 순위를
     * 두 번 정의하거나 구분자가 빠진 경우 예외를 발생시켜 잘못된 규칙으로 실제
     * 계좌 이체가 실행되지 않게 한다.</p>
     */
    private Map<Integer, BigDecimal> parseRatioRule(String rule) {
        if (rule == null || rule.trim().isEmpty()) {
            throw new IllegalStateException("미래 적립금 비율 규칙이 비어 있습니다.");
        }

        Map<Integer, BigDecimal> ratios = new HashMap<>();
        // -1 옵션은 "1:30/"처럼 마지막 값이 비어 있는 잘못된 형식도 감지하기 위해 사용한다.
        for (String entry : rule.split("/", -1)) {
            String[] parts = entry.trim().split(":", -1);
            if (parts.length != 2) {
                throw invalidRatioRule(rule);
            }
            try {
                int rank = Integer.parseInt(parts[0].trim());
                BigDecimal ratio = new BigDecimal(parts[1].trim());
                if (rank <= 0 || ratio.signum() < 0 || ratio.compareTo(ONE_HUNDRED) > 0
                        || ratios.putIfAbsent(rank, ratio) != null) {
                    throw invalidRatioRule(rule);
                }
            } catch (NumberFormatException exception) {
                throw invalidRatioRule(rule);
            }
        }
        return ratios;
    }

    /**
     * 최소 예치금에 순위 비율을 곱해 실제 이체 금액을 계산한다.
     *
     * <p>규칙에 현재 순위가 없으면 0.00원을 반환한다. 계산 결과는 금액 컬럼의
     * scale에 맞춰 소수 둘째 자리까지 HALF_UP 방식으로 반올림한다.</p>
     */
    private BigDecimal calculateSettlementAmount(BigDecimal baseAmount, BigDecimal ratio) {
        if (ratio == null) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
        }
        return baseAmount.multiply(ratio)
                .divide(ONE_HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** 금액 계산에 필요한 최소 예치금과 순위별 비율 규칙이 존재하는지 확인한다. */
    private void validateGroupSettings(GroupVO group) {
        if (group.getBaseDepositAmount() == null || group.getBaseDepositAmount().signum() < 0) {
            throw new IllegalStateException("그룹의 최소 예치금이 올바르지 않습니다.");
        }
        if (group.getFutureDepositRatioRule() == null
                || group.getFutureDepositRatioRule().trim().isEmpty()) {
            throw new IllegalStateException("그룹의 미래 적립금 비율 규칙이 없습니다.");
        }
    }

    /** 참여자별 정산과 계좌 이체에 필요한 식별자, 순위, 잔액 정보를 검증한다. */
    private void validateParticipant(RoundSettlementParticipant participant) {
        if (participant.getRoundHistoryId() == null || participant.getGroupUserId() == null
                || participant.getUserId() == null || participant.getRankNo() == null
                || participant.getRankNo() <= 0 || participant.getCurrentDepositAmount() == null
                || participant.getCurrentDepositAmount().signum() < 0
                || isBlank(participant.getDestinationKbAccountId())
                || isBlank(participant.getSourceKbAccountId())) {
            throw new IllegalStateException("라운드 참여자의 정산 정보가 올바르지 않습니다.");
        }
    }

    /**
     * 모든 참여 이력이 같은 모임통장 KB 계좌를 가리키는지 확인한다.
     * 서로 다른 모임통장이 섞여 있으면 어느 계좌에서 출금할지 확정할 수 없으므로
     * 정산을 중단한다.
     */
    private String requireCommonSourceAccount(List<RoundSettlementParticipant> participants) {
        String sourceKbAccountId = participants.get(0).getSourceKbAccountId();
        if (isBlank(sourceKbAccountId)) {
            throw new IllegalStateException("라운드의 모임통장 연결 계좌가 없습니다.");
        }
        for (RoundSettlementParticipant participant : participants) {
            if (!sourceKbAccountId.equals(participant.getSourceKbAccountId())) {
                throw new IllegalStateException("라운드 참여 이력의 모임통장이 서로 다릅니다.");
            }
        }
        return sourceKbAccountId;
    }

    /**
     * 그룹 생성자인 {@code groups.user_id}에 대응하는 {@code group_users.group_user_id}를 반환한다.
     *
     * <p>정산 요청의 {@code group_user_id}는 송금 수령자를 뜻하지 않고, 향후 실패 목록을
     * 조회하고 재처리할 책임자인 그룹장을 뜻한다. 그룹장이 group_users에 존재하지 않으면
     * 실패 내역의 소유자를 결정할 수 없으므로 실제 송금 전에 정산 전체를 중단한다.</p>
     */
    private Long requireGroupLeaderGroupUserId(String groupId, String leaderUserId) {
        if (isBlank(groupId) || isBlank(leaderUserId)) {
            throw new IllegalStateException("정산 그룹의 그룹장 정보가 올바르지 않습니다.");
        }
        Long leaderGroupUserId = roundSettlementMapper.findGroupLeaderGroupUserId(
                groupId, leaderUserId);
        if (leaderGroupUserId == null) {
            throw new IllegalStateException("그룹장의 그룹 참여 정보를 찾을 수 없습니다.");
        }
        return leaderGroupUserId;
    }

    /**
     * 모임통장 출금 또는 참여자 계좌 입금 한 건을 통합 계좌 원장에 저장한다.
     * 상대 계좌번호와 예금주도 함께 기록하여 거래 내역에서 돈의 반대편을 확인할 수 있다.
     */
    private void insertTransaction(RoundSettlementParticipant participant, Long roundId,
                                   String kbAccountId, TransactionType type, BigDecimal amount,
                                   BigDecimal balanceAfter, String idempotencyKey,
                                   String description, String anotherAccountNumber,
                                   String anotherName) {
        AccountTransactionVO transaction = new AccountTransactionVO();
        transaction.setKbAccountId(kbAccountId);
        transaction.setGroupUserId(participant.getGroupUserId());
        transaction.setRoundId(roundId);
        transaction.setTransactionType(type);
        transaction.setTransactionCategory(TransactionCategory.SETTLEMENT);
        transaction.setAmount(amount);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setDescription(description);
        transaction.setAnotherAccountNumber(anotherAccountNumber);
        transaction.setAnotherBankName(BANK_NAME);
        transaction.setAnotherName(anotherName);
        if (roundSettlementMapper.insertAccountTransaction(transaction) != 1) {
            throw new IllegalStateException("라운드 정산 거래 원장 저장에 실패했습니다.");
        }
    }

    /** 모든 독립 송금 종료 후 성공한 참여자의 출금·입금 원장을 순서대로 저장한다. */
    private void insertDeferredSettlementLedger(
            DeferredSettlementLedger deferredLedger, Long roundId, String sourceKbAccountId) {
        RoundSettlementParticipant participant = deferredLedger.participant();
        BigDecimal settlementAmount = deferredLedger.settlementAmount();
        KbTransferResult transferResult = deferredLedger.transferResult();

        // roundId, roundHistoryId, 계좌 방향을 조합한 키로 같은 정산의 중복 원장을 막는다.
        insertTransaction(participant, roundId, sourceKbAccountId,
                TransactionType.WITHDRAW, settlementAmount,
                transferResult.getSourceBalanceAfter(),
                "settlement:" + roundId + ":" + participant.getRoundHistoryId() + ":moim",
                "라운드 미래 적립금 출금", participant.getDestinationAccountNumber(),
                participant.getDestinationAccountName());
        insertTransaction(participant, roundId, participant.getDestinationKbAccountId(),
                TransactionType.DEPOSIT, settlementAmount,
                transferResult.getDestinationBalanceAfter(),
                "settlement:" + roundId + ":" + participant.getRoundHistoryId() + ":account",
                "라운드 미래 적립금 입금", participant.getSourceAccountNumber(),
                participant.getSourceAccountName());
    }

    /** 독립 송금 성공 결과를 FK 원장 저장 시점까지 유지하는 불변 값 객체. */
    private record DeferredSettlementLedger(
            RoundSettlementParticipant participant,
            BigDecimal settlementAmount,
            KbTransferResult transferResult) {
    }

    /** 형식 오류 메시지를 한 곳에서 생성하여 모든 규칙 검증 실패 응답을 통일한다. */
    private IllegalStateException invalidRatioRule(String rule) {
        return new IllegalStateException("미래 적립금 비율 규칙 형식이 올바르지 않습니다: " + rule);
    }

    /** null, 빈 문자열 및 공백 문자열을 동일하게 비어 있는 값으로 판단한다. */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
