package com.kb.youngly.service.impl;

import com.kb.youngly.dto.deposit.DepositRequest;
import com.kb.youngly.dto.deposit.DepositResponse;
import com.kb.youngly.dto.deposit.MemberDepositStatusResponse;
import com.kb.youngly.dto.transfer.KbTransferCommand;
import com.kb.youngly.dto.transfer.KbTransferResult;
import com.kb.youngly.enums.GroupUserStatus;
import com.kb.youngly.enums.TransactionCategory;
import com.kb.youngly.enums.TransactionType;
import com.kb.youngly.mapper.DepositMapper;
import com.kb.youngly.service.DepositService;
import com.kb.youngly.service.KbTransferService;
import com.kb.youngly.service.KbTransferRequestService;
import com.kb.youngly.vo.account.AccountTransactionVO;
import com.kb.youngly.vo.account.AccountVO;
import com.kb.youngly.vo.moimaccount.MoimAccountVO;
import com.kb.youngly.vo.group.GroupUserVO;
import com.kb.youngly.vo.group.GroupVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
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

    /** 한 송금에서 개인계좌 출금 원장을 구분하는 멱등성 키 접미사. */
    private static final String OUT_IDEMPOTENCY_SUFFIX = ":out";
    /** 한 송금에서 모임통장 입금 원장을 구분하는 멱등성 키 접미사. */
    private static final String IN_IDEMPOTENCY_SUFFIX = ":in";
    /**
     * 개발 중 예치금 흐름을 빠르게 확인하기 위한 임시 사용자 ID.
     * 인증 연동 전용이므로 운영 배포 전에는 요청 인증 정보로 반드시 교체해야 한다.
     */

    private final DepositMapper depositMapper;
    /** 외부 송금보다 먼저 PENDING 요청을 독립 트랜잭션으로 생성한다. */
    private final KbTransferRequestService kbTransferRequestService;
    /** 예치 과정의 실제 개인계좌→모임통장 송금과 멱등성 상태 관리를 담당한다. */
    private final KbTransferService kbTransferService;

    /**
     * 예치금을 납부한다. 같은 멱등성 키가 이미 처리되었다면 잔액을 다시 변경하지 않는다.
     *
     * <p>PENDING 송금 요청은 처리 도중 {@code REQUIRES_NEW} 트랜잭션에서 먼저 생성·커밋된다.
     * 기본 REPEATABLE_READ를 사용하면 이 메서드가 앞선 일반 SELECT에서 만든 읽기 시점과
     * 방금 커밋된 요청 행의 버전이 달라, 이후 {@code SELECT ... FOR UPDATE} 잠금 조회에서
     * 레코드 변경 충돌이 발생할 수 있다. READ_COMMITTED에서는 각 조회가 최신 커밋 상태를
     * 기준으로 실행되므로 송금 서비스가 새 PENDING 행을 잠그고 처리할 수 있다.</p>
     *
     * <p>참여자의 누적 예치금은 아래 {@code findGroupUserForUpdate}가 행 잠금을 획득한 뒤
     * 변경하므로, 격리 수준을 낮추더라도 동일 참여자의 동시 예치금 갱신은 직렬화된다.</p>
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DepositResponse deposit(String userId, String groupId, DepositRequest request) {
        String normalizedUserId =
                requireText(userId, "인증된 사용자 정보가 없습니다.");
        String normalizedGroupId = requireText(groupId, "그룹 ID는 필수입니다.");
        if (request == null) {
            throw new IllegalArgumentException("예치 요청 정보가 필요합니다.");
        }

        String sourceAccountId = requireText(request.getSourceAccountId(), "출금 계좌 ID는 필수입니다.");
        String idempotencyKey = requireText(request.getIdempotencyKey(), "멱등성 키는 필수입니다.");
        validateIdempotencyKeyLength(idempotencyKey);
        String withdrawLedgerKey = idempotencyKey + OUT_IDEMPOTENCY_SUFFIX;
        String depositLedgerKey = idempotencyKey + IN_IDEMPOTENCY_SUFFIX;
        GroupVO group = getGroup(normalizedGroupId);
        GroupUserVO member = getGroupUser(normalizedGroupId, normalizedUserId);

        /*
         * 예치 완료 여부는 요청을 대표하는 모임통장 입금 원장의 :in 키로 확인한다.
         * 원본 키는 kb_transfer_requests에서 송금 한 건의 멱등성을 보장하고,
         * :out/:in 파생 키는 account_transactions의 양쪽 원장을 각각 고유하게 식별한다.
         */
        AccountTransactionVO existing =
                depositMapper.findAccountTransactionByIdempotencyKey(depositLedgerKey);

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

        KbTransferCommand transferCommand = KbTransferCommand.builder()
                .idempotencyKey(idempotencyKey)
                .sourceKbAccountId(sourceAccount.getKbAccountId())
                .destinationKbAccountId(moimAccount.getKbAccountId())
                .amount(depositAmount)
                .transactionCategory(TransactionCategory.CHARGE)
                .groupUserId(member.getGroupUserId())
                // 예치는 정산이 아니므로 별도의 정산 수령자가 존재하지 않는다.
                .settlementReceiverId(null)
                .roundId(null)
                .build();

        /*
         * 실제 KB 송금을 호출하기 전에 PENDING 요청을 REQUIRES_NEW 트랜잭션으로 생성·커밋한다.
         * 이후 송금 과정에서 타임아웃이나 서버 종료가 발생하더라도 요청 행이 남기 때문에
         * 멱등성 키로 처리 결과를 조회하거나 재처리 여부를 판단할 수 있다.
         */
        kbTransferRequestService.createPending(transferCommand);

        /*
         * 계좌 존재·잔액 부족·동시 이체·중복 요청 검사는 KbTransferService에 위임한다.
         * 정상 결과가 반환됐다는 것은 개인계좌 출금과 모임통장 입금이 모두 성공했고
         * 앞에서 만든 PENDING 요청도 SUCCESS로 변경됐다는 의미다.
         */
        KbTransferResult transferResult = kbTransferService.transfer(transferCommand);

        /*
         * 송금 성공 후에만 Youngly가 관리하는 참여자 누적 예치금을 변경한다.
         * 이 UPDATE가 실패하면 deposit의 @Transactional 경계에 의해 Mock 송금의 계좌 변경과
         * SUCCESS 전환은 함께 롤백된다. 외부 호출 전에 별도로 커밋한 요청 행은 삭제되지 않고
         * PENDING으로 남아 후속 거래조회 또는 복구 대상이 된다.
         */
        if (depositMapper.increaseCurrentDeposit(member.getGroupUserId(), depositAmount) != 1) {
            throw new IllegalStateException("참여자 예치금 반영에 실패했습니다.");
        }

        BigDecimal depositedAmount = currentAmount.add(depositAmount);
        BigDecimal newRemainingAmount = group.getBaseDepositAmount().subtract(depositedAmount).max(BigDecimal.ZERO);
        GroupUserStatus newStatus = newRemainingAmount.signum() == 0
                ? GroupUserStatus.ACTIVE : GroupUserStatus.PENDING_DEPOSIT;
        if (depositMapper.updateGroupUserStatus(member.getGroupUserId(), newStatus) != 1) {
            throw new IllegalStateException("참여 상태 변경에 실패했습니다.");
        }

        /*
         * 동일한 원본 키를 두 행에 그대로 저장하면 account_transactions의 UNIQUE 제약에
         * 위배된다. 따라서 출금에는 :out, 입금에는 :in을 붙여 양쪽 원장을 모두 멱등하게
         * 식별하면서도 하나의 원본 송금 요청에서 파생된 거래라는 관계를 유지한다.
         */
        /*
         * 개인계좌 출금 원장에서는 돈을 받은 모임통장이 상대 계좌다. 따라서 모임통장의
         * 실제 KB 계좌번호·은행명과 서비스에 표시되는 모임통장 이름을 함께 저장한다.
         */
        insertTransaction(sourceAccount.getKbAccountId(), member.getGroupUserId(), TransactionType.WITHDRAW,
                depositAmount, transferResult.getSourceBalanceAfter(), withdrawLedgerKey, "그룹 예치금 출금",
                moimAccount.getAccountNumber(), moimAccount.getBankName(), moimAccount.getAccountName());

        /*
         * 모임통장 입금 원장에서는 돈을 보낸 개인 입출금계좌가 상대 계좌다. 출금 원장과
         * 반대 방향의 계좌번호·은행명·예금주명을 넣어 어느 계좌에서 들어온 돈인지 남긴다.
         */
        insertTransaction(moimAccount.getKbAccountId(), member.getGroupUserId(), TransactionType.DEPOSIT,
                depositAmount, transferResult.getDestinationBalanceAfter(), depositLedgerKey, "그룹 예치금 납부",
                sourceAccount.getAccountNumber(), sourceAccount.getBankName(), sourceAccount.getOwnerName());

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
                                   String description, String anotherAccountNumber,
                                   String anotherBankName, String anotherName) {
        AccountTransactionVO transaction = new AccountTransactionVO();
        transaction.setKbAccountId(kbAccountId);
        transaction.setGroupUserId(groupUserId);
        transaction.setTransactionType(type);
        transaction.setTransactionCategory(TransactionCategory.CHARGE);
        transaction.setAmount(amount);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setDescription(description);
        // 거래 방향에 따라 호출부가 전달한 실제 상대 계좌 정보를 세 컬럼에 함께 저장한다.
        transaction.setAnotherAccountNumber(anotherAccountNumber);
        transaction.setAnotherBankName(anotherBankName);
        transaction.setAnotherName(anotherName);
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

    /** 기준 예치금을 초과하는 추가 납부는 허용하되, 0원 이하 납부는 막는다. */
    private void validateDepositAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("납부 금액은 0보다 커야 합니다.");
    }

    /**
     * 원장 키 컬럼은 VARCHAR(100)이고 가장 긴 접미사도 4자이므로 원본 키를 96자로 제한한다.
     * 애플리케이션에서 먼저 검증하여 원장 INSERT 시 문자열 잘림이나 DB 오류가 발생하지 않게 한다.
     */
    private void validateIdempotencyKeyLength(String idempotencyKey) {
        int maxOriginalKeyLength = 100 - OUT_IDEMPOTENCY_SUFFIX.length();
        if (idempotencyKey.length() > maxOriginalKeyLength) {
            throw new IllegalArgumentException("멱등성 키는 " + maxOriginalKeyLength + "자 이하여야 합니다.");
        }
    }

    private BigDecimal zeroIfNull(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(message);
        return value.trim();
    }
}
