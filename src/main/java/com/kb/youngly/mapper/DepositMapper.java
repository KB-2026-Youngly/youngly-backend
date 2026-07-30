package com.kb.youngly.mapper;

import com.kb.youngly.dto.deposit.MemberDepositStatusResponse;
import com.kb.youngly.enums.GroupUserStatus;
import com.kb.youngly.vo.AccountTransactionVO;
import com.kb.youngly.vo.AccountVO;
import com.kb.youngly.vo.GroupUserVO;
import com.kb.youngly.vo.GroupVO;
import com.kb.youngly.vo.MoimAccountTransactionVO;
import com.kb.youngly.vo.MoimAccountVO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 예치금 납부와 모임통장 거래 기록을 위한 MyBatis Mapper.
 *
 * <p>잔액 변경 메서드는 반드시 {@code DepositServiceImpl}의 트랜잭션 안에서
 * 호출해야 개인 계좌·모임통장·참여자 예치금·거래 이력이 함께 반영된다.</p>
 */
public interface DepositMapper {

    /** 기준 예치금과 연결 모임통장 정보를 포함한 그룹을 조회한다. */
    GroupVO findGroupById(String groupId);

    /** 조회 권한과 현재 예치금 확인에 사용하는 일반 참여자 조회. */
    GroupUserVO findGroupUser(@Param("groupId") String groupId,
                              @Param("userId") String userId);

    /** 현재 예치금 동시 변경을 방지하기 위한 잠금 조회. */
    GroupUserVO findGroupUserForUpdate(@Param("groupId") String groupId,
                                       @Param("userId") String userId);

    /** 출금 계좌 소유 여부 확인 및 잔액 일관성을 위한 잠금 조회. */
    AccountVO findAccountForUpdate(@Param("accountId") String accountId,
                                   @Param("userId") String userId);

    /** 모임통장 입금과 balance_after 계산을 위한 잠금 조회. */
    MoimAccountVO findMoimAccountForUpdate(String moimAccountId);

    /** 잔액이 충분할 때만 개인 계좌에서 출금한다. 실패하면 0을 반환한다. */
    int withdrawFromAccount(@Param("accountId") String accountId,
                            @Param("userId") String userId,
                            @Param("amount") BigDecimal amount);

    /** 모임통장 잔액을 예치금만큼 증가시킨다. */
    int increaseMoimAccountBalance(@Param("moimAccountId") String moimAccountId,
                                   @Param("amount") BigDecimal amount);

    /** 참여자별 현재 예치금에 이번 납부 금액을 더한다. */
    int increaseCurrentDeposit(@Param("groupUserId") Long groupUserId,
                               @Param("amount") BigDecimal amount);

    /** 전액 납부 여부에 따라 참여 상태를 ACTIVE 또는 PENDING_DEPOSIT으로 갱신한다. */
    int updateGroupUserStatus(@Param("groupUserId") Long groupUserId,
                              @Param("status") GroupUserStatus status);

    /** 개인 계좌 출금 거래 이력을 저장한다. */
    int insertAccountTransaction(AccountTransactionVO transaction);

    /** 모임통장 입금·출금 거래 이력을 저장한다. */
    int insertMoimAccountTransaction(MoimAccountTransactionVO transaction);

    /** 같은 요청 키로 이미 완료된 모임통장 거래가 있는지 조회한다. */
    MoimAccountTransactionVO findMoimTransactionByIdempotencyKey(String idempotencyKey);

    /** 그룹 참여자 전체의 프로필과 예치금 현황을 조회한다. */
    List<MemberDepositStatusResponse> findMemberDepositStatuses(String groupId);
}
