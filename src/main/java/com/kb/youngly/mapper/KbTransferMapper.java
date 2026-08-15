package com.kb.youngly.mapper;

import com.kb.youngly.vo.transfer.KbTransferRequestVO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mock KB 계좌 이체와 송금 요청 상태 저장을 담당하는 Mapper.
 *
 * <p>계좌 잔액 변경 SQL을 정산 Mapper에서 분리하여, 향후 실제 KB API로 교체할 때
 * 정산 비즈니스 쿼리와 외부 금융 연동 경계가 섞이지 않게 한다.</p>
 */
public interface KbTransferMapper {

    /**
     * 로그인 사용자가 실제 그룹장이면서 요청 소유자인 정산 요청을 잠가 조회한다.
     * 요청 ID와 roundId를 함께 검사하여 다른 라운드의 요청을 재시도하지 못하게 한다.
     */
    KbTransferRequestVO findOwnedSettlementRequestForUpdate(
            @Param("kbTransferRequestId") Long kbTransferRequestId,
            @Param("roundId") Long roundId,
            @Param("userId") String userId);

    /** 기존 FAILED 요청을 새 행 생성 없이 다시 실행 가능한 PENDING으로 전환한다. */
    int markFailedRequestPending(
            @Param("kbTransferRequestId") Long kbTransferRequestId);

    /** 재시도 결과를 응답하기 위해 송금 요청 한 건의 최신 상태를 조회한다. */
    KbTransferRequestVO findById(
            @Param("kbTransferRequestId") Long kbTransferRequestId);

    /**
     * 로그인 사용자가 관리 주체로 기록된 특정 라운드의 정산 송금 요청을 조회한다.
     * 사용자 ID를 직접 요청 테이블과 비교하지 않고, 라운드가 속한 그룹의
     * {@code group_users.group_user_id}를 거쳐 요청 소유권을 확인한다.
     */
    List<KbTransferRequestVO> findSettlementRequestsByRoundAndUser(
            @Param("roundId") Long roundId,
            @Param("userId") String userId);

    /** 멱등성 키에 해당하는 기존 요청을 잠그고 중복 송금을 방지한다. */
    KbTransferRequestVO findByIdempotencyKeyForUpdate(
            @Param("idempotencyKey") String idempotencyKey);

    /** 외부 송금 호출 전에 PENDING 요청을 먼저 생성한다. */
    int insertPending(KbTransferRequestVO request);

    /** 계좌 잔액을 변경 전 잠그고 현재 값을 반환한다. */
    BigDecimal findKbAccountBalanceForUpdate(@Param("kbAccountId") String kbAccountId);

    /** 잔액이 충분한 경우에만 출금하여 음수 잔액을 이중으로 방지한다. */
    int withdraw(@Param("kbAccountId") String kbAccountId,
                 @Param("amount") BigDecimal amount);

    /** 입금 계좌 잔액을 증가시킨다. */
    int deposit(@Param("kbAccountId") String kbAccountId,
                @Param("amount") BigDecimal amount);

    /** 양쪽 계좌 변경이 끝난 요청에 성공 상태와 성공 당시 잔액을 저장한다. */
    int markSuccess(@Param("kbTransferRequestId") Long kbTransferRequestId,
                    @Param("kbTransactionId") String kbTransactionId,
                    @Param("sourceBalanceAfter") BigDecimal sourceBalanceAfter,
                    @Param("destinationBalanceAfter") BigDecimal destinationBalanceAfter);

    /**
     * 원래 송금 트랜잭션이 롤백된 뒤 별도 트랜잭션에서 실패 내역을 생성하거나 갱신한다.
     * 성공이 이미 확정된 요청은 SQL에서 보호하여 실패 상태로 덮어쓰지 않는다.
     */
    int upsertFailure(KbTransferRequestVO request);
}
