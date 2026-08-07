package com.kb.youngly.service.impl;

import com.kb.youngly.dto.round.RoundSettlementParticipant;
import com.kb.youngly.enums.GroupStatus;
import com.kb.youngly.enums.TransactionCategory;
import com.kb.youngly.enums.TransactionType;
import com.kb.youngly.mapper.RoundMapper;
import com.kb.youngly.mapper.RoundSettlementMapper;
import com.kb.youngly.service.RoundSettlementService;
import com.kb.youngly.vo.account.AccountTransactionVO;
import com.kb.youngly.vo.group.GroupVO;
import com.kb.youngly.vo.round.RoundVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 순위별 미래 적립금을 모임통장에서 참여자 계좌로 이체하는 서비스.
 *
 * <p>한 그룹의 계좌 잔액, 개인별 예치금, 양쪽 거래 원장, 라운드 이력 및
 * 라운드 상태를 하나의 트랜잭션으로 변경한다. 어느 한 단계라도 실패하면
 * 모든 변경을 롤백하므로 일부 참여자만 정산되는 상태가 남지 않는다.</p>
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
    /** 정산 대상·계좌 잠금 조회와 실제 잔액 및 이력 변경을 담당한다. */
    private final RoundSettlementMapper roundSettlementMapper;

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
     * <p>메서드가 정상 종료되면 커밋되면서 {@code FOR UPDATE} 잠금이 해제된다.
     * 중간에 예외가 발생하면 계좌 잔액을 포함한 모든 변경이 롤백되고 잠금도
     * 해제되므로 일부 참여자만 정산된 결과는 남지 않는다.</p>
     *
     * @param groupId 정산 대상 라운드가 속한 그룹 ID
     * @param settlementDate 한국시간 기준 정산 실행일
     * @return 정산을 완료했으면 true, 이미 처리됐거나 대상이 아니면 false
     */
    @Override
    @Transactional
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
        List<RoundSettlementParticipant> participants =
                roundSettlementMapper.findParticipantsForUpdate(round.getRoundId());
        if (participants.isEmpty()) {
            throw new IllegalStateException("정산할 라운드 참여 이력이 없습니다.");
        }

        // 모든 참여자가 공유하는 모임통장을 한 번 잠그고 이후 잔액을 메모리에서 누적한다.
        String sourceKbAccountId = requireCommonSourceAccount(participants);
        BigDecimal sourceBalance = requireBalance(sourceKbAccountId);

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
                if (sourceBalance.compareTo(settlementAmount) < 0) {
                    throw new IllegalStateException("모임통장 잔액이 라운드 정산액보다 부족합니다.");
                }

                // 수령 계좌를 잠가 입금 직후 balance_after를 정확히 원장에 기록한다.
                // 입금 계좌 잔액 플러스, 출금 계좌 잔액 마이너스, 개인 예치금 현황 마이너스 => 이에 따른 최소 예치금 충족 여부 판정
                BigDecimal destinationBalance = requireBalance(participant.getDestinationKbAccountId());
                if (roundSettlementMapper.withdrawFromKbAccount(sourceKbAccountId, settlementAmount) != 1
                        || roundSettlementMapper.increaseKbAccountBalance(
                                participant.getDestinationKbAccountId(), settlementAmount) != 1
                        || roundSettlementMapper.deductCurrentDepositAndUpdateStatus(
                                participant.getGroupUserId(), settlementAmount,
                                group.getBaseDepositAmount()) != 1) {
                    throw new IllegalStateException("참여자 정산 잔액 반영에 실패했습니다.");
                }

                BigDecimal sourceBalanceAfter = sourceBalance.subtract(settlementAmount);
                BigDecimal destinationBalanceAfter = destinationBalance.add(settlementAmount);

                // 실제 돈의 흐름과 동일하게 모임통장 출금, 개인계좌 입금 원장을 각각 만든다.
                // roundId, roundHistoryId, 계좌 방향을 조합한 키로 같은 정산의 중복 원장을 막는다.
                insertTransaction(participant, round.getRoundId(), sourceKbAccountId,
                        TransactionType.WITHDRAW, settlementAmount, sourceBalanceAfter,
                        "settlement:" + round.getRoundId() + ":"
                                + participant.getRoundHistoryId() + ":moim",
                        "라운드 미래 적립금 출금", participant.getDestinationAccountNumber(),
                        participant.getDestinationAccountName());
                insertTransaction(participant, round.getRoundId(),
                        participant.getDestinationKbAccountId(), TransactionType.DEPOSIT,
                        settlementAmount, destinationBalanceAfter,
                        "settlement:" + round.getRoundId() + ":"
                                + participant.getRoundHistoryId() + ":account",
                        "라운드 미래 적립금 입금", participant.getSourceAccountNumber(),
                        participant.getSourceAccountName());

                // 다음 참여자는 직전 출금이 반영된 잔액을 기준으로 부족 여부와 원장을 계산한다.
                sourceBalance = sourceBalanceAfter;
            }

            // 규칙에 없는 순위도 0원으로 확정하고 이전 실패 대응값을 비운다.
            if (roundSettlementMapper.updateRoundHistorySettlement(
                    participant.getRoundHistoryId(), settlementAmount) != 1) {
                throw new IllegalStateException("라운드 참여 이력 정산 반영에 실패했습니다.");
            }
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
     * KB 계좌 행을 {@code FOR UPDATE}로 잠그고 현재 잔액을 반환한다.
     * 반환된 잔액은 실제 UPDATE와 거래 원장의 balance_after 계산에 함께 사용된다.
     */
    private BigDecimal requireBalance(String kbAccountId) {
        BigDecimal balance = roundSettlementMapper.findKbAccountBalanceForUpdate(kbAccountId);
        if (balance == null) {
            throw new IllegalStateException("정산 계좌의 KB 계좌 정보를 찾을 수 없습니다.");
        }
        return balance;
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

    /** 형식 오류 메시지를 한 곳에서 생성하여 모든 규칙 검증 실패 응답을 통일한다. */
    private IllegalStateException invalidRatioRule(String rule) {
        return new IllegalStateException("미래 적립금 비율 규칙 형식이 올바르지 않습니다: " + rule);
    }

    /** null, 빈 문자열 및 공백 문자열을 동일하게 비어 있는 값으로 판단한다. */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
