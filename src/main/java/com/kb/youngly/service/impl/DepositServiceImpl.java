package com.kb.youngly.service.impl;

import com.kb.youngly.dto.deposit.DepositRequest;
import com.kb.youngly.dto.deposit.DepositResponse;
import com.kb.youngly.dto.deposit.MemberDepositStatusResponse;
import com.kb.youngly.enums.GroupUserStatus;
import com.kb.youngly.enums.TransactionCategory;
import com.kb.youngly.enums.TransactionType;
import com.kb.youngly.mapper.DepositMapper;
import com.kb.youngly.service.DepositService;
import com.kb.youngly.vo.account.AccountTransactionVO;
import com.kb.youngly.vo.account.AccountVO;
import com.kb.youngly.vo.moimaccount.MoimAccountVO;
import com.kb.youngly.vo.group.GroupUserVO;
import com.kb.youngly.vo.group.GroupVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 개인 KB 계좌에서 모임통장으로 예치금을 이체하는 서비스 구현체.
 *
 * <p>계좌 잔액, 참여자 예치금, 양쪽 거래 원장은 하나의 트랜잭션으로 처리한다.
 * 어느 단계라도 실패하면 모두 롤백되어 금액과 거래 이력이 어긋나지 않는다.</p>
 */
@Service
@RequiredArgsConstructor
public class DepositServiceImpl implements DepositService {

    private static final String BANK_NAME = "국민";
    /**
     * 개발 중 예치금 흐름을 빠르게 확인하기 위한 임시 사용자 ID.
     * 인증 연동 전용이므로 운영 배포 전에는 요청 인증 정보로 반드시 교체해야 한다.
     */

    private final DepositMapper depositMapper;

    /**
     * 예치금을 납부한다. 같은 멱등성 키가 이미 처리되었다면 잔액을 다시 변경하지 않는다.
     */
    @Override
    @Transactional
    public DepositResponse deposit(String userId, String groupId, DepositRequest request) {
        String normalizedUserId =
                requireText(userId, "인증된 사용자 정보가 없습니다.");
        String normalizedGroupId = requireText(groupId, "그룹 ID는 필수입니다.");
        if (request == null) {
            throw new IllegalArgumentException("예치 요청 정보가 필요합니다.");
        }

        String sourceAccountId = requireText(request.getSourceAccountId(), "출금 계좌 ID는 필수입니다.");
        String idempotencyKey = requireText(request.getIdempotencyKey(), "멱등성 키는 필수입니다.");
        GroupVO group = getGroup(normalizedGroupId);
        GroupUserVO member = getGroupUser(normalizedGroupId, normalizedUserId);

        // 이미 완료된 요청은 현재 상태만 돌려주며 이체를 반복하지 않는다.
        AccountTransactionVO existing = depositMapper.findAccountTransactionByIdempotencyKey(idempotencyKey);

        if (existing != null) {
            if (!member.getGroupUserId().equals(existing.getGroupUserId())) {
                throw new IllegalArgumentException("다른 사용자의 예치 요청 키입니다.");
            }
            return toResponse(group, member, existing.getTransactionCategory());
        }

        // 누적 예치금을 읽고 변경하는 동안 참여자 행을 잠근다.
        member = depositMapper.findGroupUserForUpdate(normalizedGroupId, normalizedUserId);
        validateDepositableMember(member);

        BigDecimal currentAmount = zeroIfNull(member.getCurrentDepositAmount());
        BigDecimal remainingAmount = group.getBaseDepositAmount().subtract(currentAmount).max(BigDecimal.ZERO);

        // 금액을 생략하면 남은 기준 예치금을 납부한다.
        // 이미 기준 금액을 채운 뒤에는 추가 납부 금액을 명시해야 한다.
        BigDecimal depositAmount = request.getAmount() == null ? remainingAmount : request.getAmount();
        validateDepositAmount(depositAmount);

        AccountVO sourceAccount = depositMapper.findAccountByIdAndUserId(sourceAccountId, normalizedUserId);
        if (sourceAccount == null) {
            throw new IllegalArgumentException("출금 계좌를 찾을 수 없습니다.");
        }
        if (depositMapper.countDepositKbAccount(sourceAccount.getKbAccountId()) != 1) {
            throw new IllegalArgumentException("입출금 계좌에서만 예치할 수 있습니다.");
        }
        MoimAccountVO moimAccount = depositMapper.findMoimAccountById(group.getMoimAccountId());
        if (moimAccount == null) {
            throw new IllegalStateException("그룹에 연결된 모임통장을 찾을 수 없습니다.");
        }

        // 두 KB 계좌를 잠가 balance_after를 정확히 기록한다.
        BigDecimal sourceBalance = requireBalance(sourceAccount.getKbAccountId());
        if (sourceBalance.compareTo(depositAmount) < 0) {
            throw new IllegalArgumentException("출금 계좌의 잔액이 부족합니다.");
        }
        BigDecimal moimBalance = requireBalance(moimAccount.getKbAccountId());

        if (depositMapper.withdrawFromKbAccount(sourceAccount.getKbAccountId(), depositAmount) != 1
                || depositMapper.increaseKbAccountBalance(moimAccount.getKbAccountId(), depositAmount) != 1
                || depositMapper.increaseCurrentDeposit(member.getGroupUserId(), depositAmount) != 1) {
            throw new IllegalStateException("예치금 처리에 실패했습니다.");
        }

        BigDecimal depositedAmount = currentAmount.add(depositAmount);
        BigDecimal newRemainingAmount = group.getBaseDepositAmount().subtract(depositedAmount).max(BigDecimal.ZERO);
        GroupUserStatus newStatus = newRemainingAmount.signum() == 0
                ? GroupUserStatus.ACTIVE : GroupUserStatus.PENDING_DEPOSIT;
        if (depositMapper.updateGroupUserStatus(member.getGroupUserId(), newStatus) != 1) {
            throw new IllegalStateException("참여 상태 변경에 실패했습니다.");
        }

        // 출금 원장에는 멱등성 키를 저장하지 않는다. 한 요청의 입금 원장만 고유해야 한다.
        insertTransaction(sourceAccount.getKbAccountId(), member.getGroupUserId(), TransactionType.WITHDRAW,
                depositAmount, sourceBalance.subtract(depositAmount), null, "그룹 예치금 출금");
        insertTransaction(moimAccount.getKbAccountId(), member.getGroupUserId(), TransactionType.DEPOSIT,
                depositAmount, moimBalance.add(depositAmount), idempotencyKey, "그룹 예치금 납부");

        return response(normalizedGroupId, normalizedUserId, group.getBaseDepositAmount(), depositedAmount,
                newRemainingAmount, newStatus, TransactionCategory.CHARGE);
    }

    /** 로그인 사용자의 예치금 현황을 조회한다. */
    @Override
    @Transactional(readOnly = true)
    public DepositResponse getMyDeposit(String userId, String groupId) {
        String normalizedUserId =
                requireText(userId, "인증된 사용자 정보가 없습니다.");
        String normalizedGroupId = requireText(groupId, "그룹 ID는 필수입니다.");
        return toResponse(getGroup(normalizedGroupId), getGroupUser(normalizedGroupId, normalizedUserId), null);
    }

    /** 요청자가 그룹 참여자인지 확인한 뒤 전체 참여자의 예치 현황을 반환한다. */
    @Override
    @Transactional(readOnly = true)
    public List<MemberDepositStatusResponse> getMemberDepositStatuses(String requesterUserId, String groupId) {
        String normalizedUserId =
                requireText(requesterUserId, "인증된 사용자 정보가 없습니다.");
        String normalizedGroupId = requireText(groupId, "그룹 ID는 필수입니다.");
        getGroup(normalizedGroupId);
        getGroupUser(normalizedGroupId, normalizedUserId);
        return depositMapper.findMemberDepositStatuses(normalizedGroupId);
    }


    private void insertTransaction(String kbAccountId, Long groupUserId, TransactionType type,
                                   BigDecimal amount, BigDecimal balanceAfter, String idempotencyKey,
                                   String description) {
        AccountTransactionVO transaction = new AccountTransactionVO();
        transaction.setKbAccountId(kbAccountId);
        transaction.setGroupUserId(groupUserId);
        transaction.setTransactionType(type);
        transaction.setTransactionCategory(TransactionCategory.CHARGE);
        transaction.setAmount(amount);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setDescription(description);
        transaction.setAnotherBankName(BANK_NAME);
        depositMapper.insertAccountTransaction(transaction);
    }

    private DepositResponse toResponse(GroupVO group, GroupUserVO member, TransactionCategory category) {
        BigDecimal depositedAmount = zeroIfNull(member.getCurrentDepositAmount());
        return response(group.getGroupId(), member.getUserId(), group.getBaseDepositAmount(), depositedAmount,
                group.getBaseDepositAmount().subtract(depositedAmount).max(BigDecimal.ZERO),
                member.getGroupUserStatus(), category);
    }

    private DepositResponse response(String groupId, String userId, BigDecimal requiredAmount,
                                     BigDecimal depositedAmount, BigDecimal remainingAmount,
                                     GroupUserStatus status, TransactionCategory category) {
        return DepositResponse.builder().groupId(groupId).userId(userId).requiredAmount(requiredAmount)
                .depositedAmount(depositedAmount).remainingAmount(remainingAmount)
                .groupUserStatus(status).transactionCategory(category).build();
    }

    private GroupVO getGroup(String groupId) {
        GroupVO group = depositMapper.findGroupById(groupId);
        if (group == null) throw new IllegalArgumentException("그룹을 찾을 수 없습니다.");
        if (group.getMoimAccountId() == null || group.getBaseDepositAmount() == null
                || group.getBaseDepositAmount().signum() < 0) {
            throw new IllegalStateException("그룹의 예치금 설정이 올바르지 않습니다.");
        }
        return group;
    }

    private GroupUserVO getGroupUser(String groupId, String userId) {
        GroupUserVO member = depositMapper.findGroupUser(groupId, userId);
        if (member == null) throw new IllegalArgumentException("그룹 참여 정보를 찾을 수 없습니다.");
        return member;
    }

    private void validateDepositableMember(GroupUserVO member) {
        if (member == null) throw new IllegalArgumentException("그룹 참여 정보를 찾을 수 없습니다.");
        if (member.getGroupUserStatus() != GroupUserStatus.PENDING_DEPOSIT
                && member.getGroupUserStatus() != GroupUserStatus.ACTIVE) {
            throw new IllegalArgumentException("현재 상태에서는 예치금을 납부할 수 없습니다.");
        }
    }

    private BigDecimal requireBalance(String kbAccountId) {
        BigDecimal balance = depositMapper.findKbAccountBalanceForUpdate(kbAccountId);
        if (balance == null) throw new IllegalStateException("연결된 KB 계좌를 찾을 수 없습니다.");
        return balance;
    }

    /** 기준 예치금을 초과하는 추가 납부는 허용하되, 0원 이하 납부는 막는다. */
    private void validateDepositAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("납부 금액은 0보다 커야 합니다.");
    }

    private BigDecimal zeroIfNull(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(message);
        return value.trim();
    }
}
