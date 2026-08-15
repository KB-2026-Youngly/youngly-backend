package com.kb.youngly.mapper;

import com.kb.youngly.dto.deposit.MemberDepositStatusResponse;
import com.kb.youngly.enums.GroupUserStatus;
import com.kb.youngly.vo.account.AccountTransactionVO;
import com.kb.youngly.vo.account.AccountVO;
import com.kb.youngly.vo.moimaccount.MoimAccountVO;
import com.kb.youngly.vo.group.GroupUserVO;
import com.kb.youngly.vo.group.GroupVO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 예치금 납부에 필요한 계좌·참여자·거래 원장 MyBatis Mapper.
 *
 * <p>참여자 예치 상태와 예치 거래원장을 담당한다. KB 계좌 잠금과 실제 잔액 변경은
 * 예치·정산에서 공통으로 사용하는 {@link KbTransferMapper}에 위임한다.</p>
 */
public interface DepositMapper {

    GroupVO findGroupById(String groupId);

    GroupUserVO findGroupUser(@Param("groupId") String groupId, @Param("userId") String userId);

    GroupUserVO findGroupUserForUpdate(@Param("groupId") String groupId, @Param("userId") String userId);

    /** 사용자 소유 계좌와 연결된 KB 계좌 정보를 함께 조회한다. */
    AccountVO findAccountByIdAndUserId(@Param("accountId") String accountId, @Param("userId") String userId);

    MoimAccountVO findMoimAccountById(String moimAccountId);

    /** 개인 출금 계좌가 입출금(DEPOSIT) 계좌인지 확인한다. */
    int countDepositKbAccount(String kbAccountId);

    int increaseCurrentDeposit(@Param("groupUserId") Long groupUserId, @Param("amount") BigDecimal amount);

    int updateGroupUserStatus(@Param("groupUserId") Long groupUserId, @Param("status") GroupUserStatus status);

    int insertAccountTransaction(AccountTransactionVO transaction);

    /**
     * 계좌별 파생 멱등성 키로 거래원장을 조회한다.
     * 예치 완료 여부는 원본 요청 키에 {@code :in}을 붙인 모임통장 입금 원장으로 확인한다.
     */
    AccountTransactionVO findAccountTransactionByIdempotencyKey(String idempotencyKey);

    List<MemberDepositStatusResponse> findMemberDepositStatuses(String groupId);
}
