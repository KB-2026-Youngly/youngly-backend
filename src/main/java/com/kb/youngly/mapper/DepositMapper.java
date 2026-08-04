package com.kb.youngly.mapper;

import com.kb.youngly.dto.deposit.MemberDepositStatusResponse;
import com.kb.youngly.enums.GroupUserStatus;
import com.kb.youngly.vo.account.AccountTransactionVO;
import com.kb.youngly.vo.account.AccountVO;
import com.kb.youngly.vo.account.MoimAccountVO;
import com.kb.youngly.vo.group.GroupUserVO;
import com.kb.youngly.vo.group.GroupVO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 예치금 납부에 필요한 계좌·참여자·거래 원장 MyBatis Mapper.
 *
 * <p>실제 금액은 {@code kb_accounts.balance}에서 관리한다. 모든 잠금 조회와
 * 잔액 변경은 서비스 트랜잭션 안에서만 호출해야 한다.</p>
 */
public interface DepositMapper {

    GroupVO findGroupById(String groupId);

    GroupUserVO findGroupUser(@Param("groupId") String groupId, @Param("userId") String userId);

    GroupUserVO findGroupUserForUpdate(@Param("groupId") String groupId, @Param("userId") String userId);

    /** 사용자 소유 계좌와 연결된 KB 계좌 정보를 함께 조회한다. */
    AccountVO findAccountByIdAndUserId(@Param("accountId") String accountId, @Param("userId") String userId);

    MoimAccountVO findMoimAccountById(String moimAccountId);

    /** KB 계좌 잔액을 잠근 뒤 반환한다. */
    BigDecimal findKbAccountBalanceForUpdate(String kbAccountId);

    /** 개인 출금 계좌가 입출금(DEPOSIT) 계좌인지 확인한다. */
    int countDepositKbAccount(String kbAccountId);

    /** 잔액이 충분할 때만 KB 계좌에서 출금한다. */
    int withdrawFromKbAccount(@Param("kbAccountId") String kbAccountId, @Param("amount") BigDecimal amount);

    int increaseKbAccountBalance(@Param("kbAccountId") String kbAccountId, @Param("amount") BigDecimal amount);

    int increaseCurrentDeposit(@Param("groupUserId") Long groupUserId, @Param("amount") BigDecimal amount);

    int updateGroupUserStatus(@Param("groupUserId") Long groupUserId, @Param("status") GroupUserStatus status);

    int insertAccountTransaction(AccountTransactionVO transaction);

    /** 멱등성 키는 모임통장 입금 원장에만 저장해 한 요청의 두 거래를 구분한다. */
    AccountTransactionVO findAccountTransactionByIdempotencyKey(String idempotencyKey);

    List<MemberDepositStatusResponse> findMemberDepositStatuses(String groupId);
}
