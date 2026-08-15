package com.kb.youngly.service.impl;

import com.kb.youngly.dto.transfer.KbTransferCommand;
import com.kb.youngly.dto.transfer.KbTransferResult;
import com.kb.youngly.enums.KbTransferStatus;
import com.kb.youngly.enums.TransactionCategory;
import com.kb.youngly.mapper.KbTransferMapper;
import com.kb.youngly.service.KbTransferFailureRecorder;
import com.kb.youngly.service.KbTransferService;
import com.kb.youngly.vo.transfer.KbTransferRequestVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * {@code kb_accounts}를 KB 계정계처럼 사용하는 개발용 송금 구현체.
 *
 * <p>정산 서비스는 이 구현체가 DB를 사용한다는 사실을 알 필요가 없다. 실제 KB 송금 API가
 * 준비되면 {@link KbTransferService}의 다른 구현체로 교체하고, 이 클래스는 로컬·테스트
 * 프로필에서만 사용하도록 분리할 수 있다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MockKbTransferServiceImpl implements KbTransferService {

    private final KbTransferMapper kbTransferMapper;
    /** 본 송금 트랜잭션이 롤백된 뒤 독립 트랜잭션으로 실패 내역을 남긴다. */
    private final KbTransferFailureRecorder failureRecorder;

    /**
     * 미리 커밋된 PENDING 요청을 멱등하게 실행하고 성공 당시 양쪽 계좌 잔액을 반환한다.
     *
     * <p>기본 전파 방식인 REQUIRED를 사용하므로 라운드 정산에서 호출되면 정산 트랜잭션에
     * 참여한다. 따라서 계좌 입금 이후 예치금 또는 원장 저장이 실패하면 계좌 변경과 SUCCESS
     * 전환이 함께 롤백된다. 단, 외부 호출 전에 별도로 커밋된 요청 행은 사라지지 않고 PENDING으로
     * 남아 결과 확인 및 복구의 기준이 된다.</p>
     *
     * <p>PENDING 생성은 이 메서드의 책임이 아니다. 호출자는 반드시
     * {@link com.kb.youngly.service.KbTransferRequestService#createPending(KbTransferCommand)}을
     * 먼저 호출해 요청을 독립 트랜잭션으로 커밋해야 한다. 이 순서 덕분에 실제 외부 API 호출 중
     * 장애가 발생해도 송금을 시도할 요청 자체는 사라지지 않는다.</p>
     */
    @Override
    @Transactional
    public KbTransferResult transfer(KbTransferCommand command) {
        validateCommand(command);

        /*
         * transfer는 요청을 새로 만들지 않고 호출자가 먼저 커밋한 요청만 처리한다.
         * 요청이 없다면 외부 호출보다 PENDING을 먼저 남긴다는 계약을 위반한 것이므로 중단한다.
         */
        KbTransferRequestVO request = kbTransferMapper.findByIdempotencyKeyForUpdate(
                command.getIdempotencyKey().trim());
        if (request == null) {
            throw new IllegalStateException("KB 송금 PENDING 요청이 먼저 생성되어야 합니다.");
        }
        validateSameRequest(request, command);

        /*
         * SUCCESS는 최초 성공 결과를 반환하고, FAILED/UNKNOWN은 재송금하지 않는다.
         * 실제 실행 가능한 상태는 호출자가 미리 만든 PENDING뿐이다.
         */
        if (request.getTransferStatus() != KbTransferStatus.PENDING) {
            return resultFromExisting(request);
        }

        /*
         * 이 시점부터 발생하는 오류는 유효한 송금 요청을 실제로 처리하던 중 발생한 실패다.
         * 먼저 현재 트랜잭션을 전부 롤백하여 계좌의 부분 변경을 제거하고, 롤백 완료 후
         * 별도 트랜잭션에서 FAILED 내역을 저장하도록 예약한다.
         */
        try {
            return executePendingTransfer(request);
        } catch (RuntimeException exception) {
            scheduleFailureRecording(
                    request,
                    determineFailureCode(exception),
                    exception.getMessage());
            throw exception;
        }
    }

    /**
     * PENDING 요청에 대해 실제 Mock 계좌 잔액을 변경하고 SUCCESS를 확정한다.
     * 호출부의 try/catch와 분리하여 실패 기록 예약 로직이 모든 처리 단계의 예외를 포착하게 한다.
     */
    private KbTransferResult executePendingTransfer(KbTransferRequestVO request) {

        /*
         * 서로 반대 방향의 송금이 동시에 실행될 때 A→B는 A부터, B→A는 B부터 잠그면
         * 교착 상태가 생길 수 있다. 출금·입금 방향과 무관하게 계좌 ID 사전순으로 잠가
         * 모든 송금이 동일한 잠금 순서를 사용하게 한다.
         */
        BigDecimal sourceBalance;
        BigDecimal destinationBalance;
        if (request.getSourceKbAccountId().compareTo(request.getDestinationKbAccountId()) < 0) {
            sourceBalance = requireBalance(request.getSourceKbAccountId(), "출금");
            destinationBalance = requireBalance(request.getDestinationKbAccountId(), "입금");
        } else {
            destinationBalance = requireBalance(request.getDestinationKbAccountId(), "입금");
            sourceBalance = requireBalance(request.getSourceKbAccountId(), "출금");
        }

        // 잔액 부족은 외부 장애가 아니라 KB가 송금을 거절할 명확한 업무 실패에 해당한다.
        if (sourceBalance.compareTo(request.getAmount()) < 0) {
            throw new IllegalStateException("출금 계좌의 잔액이 이체 금액보다 부족합니다.");
        }

        /*
         * 출금 SQL도 balance >= amount 조건을 가지므로 사전 검증과 DB 조건이 함께 음수 잔액을
         * 방지한다. 입금까지 정확히 한 행씩 변경되지 않으면 예외로 전체 트랜잭션을 롤백한다.
         */
        if (kbTransferMapper.withdraw(request.getSourceKbAccountId(), request.getAmount()) != 1) {
            throw new IllegalStateException("KB 출금 계좌 잔액 반영에 실패했습니다.");
        }
        if (kbTransferMapper.deposit(request.getDestinationKbAccountId(), request.getAmount()) != 1) {
            throw new IllegalStateException("KB 입금 계좌 잔액 반영에 실패했습니다.");
        }

        BigDecimal sourceBalanceAfter = sourceBalance.subtract(request.getAmount());
        BigDecimal destinationBalanceAfter = destinationBalance.add(request.getAmount());

        // 실제 API의 거래번호를 흉내 내되 요청 PK로 유일하고 재현 가능한 Mock 거래번호를 만든다.
        String kbTransactionId = "MOCK-KB-" + request.getKbTransferRequestId();
        if (kbTransferMapper.markSuccess(
                request.getKbTransferRequestId(), kbTransactionId,
                sourceBalanceAfter, destinationBalanceAfter) != 1) {
            throw new IllegalStateException("KB 송금 성공 상태 저장에 실패했습니다.");
        }

        return new KbTransferResult(
                request.getKbTransferRequestId(), kbTransactionId,
                sourceBalanceAfter, destinationBalanceAfter);
    }

    /**
     * 현재 송금 트랜잭션이 실제로 롤백된 뒤 FAILED 내역을 저장하도록 콜백을 등록한다.
     *
     * <p>예외를 잡은 즉시 실패 행을 저장하면 아직 롤백되지 않은 PENDING 행의 유니크 키 및
     * FK 잠금과 충돌할 수 있다. {@code afterCompletion}에서 롤백 완료를 확인한 다음
     * {@link KbTransferFailureRecorder}의 REQUIRES_NEW 트랜잭션을 호출하면 기존 잠금과
     * 독립적으로 실패 행을 안전하게 저장할 수 있다.</p>
     */
    private void scheduleFailureRecording(KbTransferRequestVO request,
                                          String failureCode,
                                          String failureMessage) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            /*
             * transfer()는 @Transactional이므로 정상적인 Spring 호출에서는 이 분기로 오지 않는다.
             * 프록시를 거치지 않은 잘못된 호출에서 원래 예외를 숨기지 않으면서 원인을 로그로 남긴다.
             */
            log.error("트랜잭션 동기화가 없어 KB 송금 실패 기록을 예약할 수 없습니다. idempotencyKey={}",
                    request.getIdempotencyKey());
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        // 커밋된 트랜잭션에는 실패 기록을 만들지 않고, 실제 롤백된 경우에만 저장한다.
                        if (status != TransactionSynchronization.STATUS_ROLLED_BACK) {
                            return;
                        }
                        try {
                            failureRecorder.record(
                                    request,
                                    KbTransferStatus.FAILED,
                                    failureCode,
                                    failureMessage);
                        } catch (RuntimeException recordException) {
                            /*
                             * afterCompletion에서 발생한 기록 오류로 원래 송금 예외를 대체하지 않는다.
                             * 운영 환경에서는 이 로그를 알림 대상으로 연결해 수동 복구할 수 있어야 한다.
                             */
                            log.error("KB 송금 실패 내역 저장 중 오류가 발생했습니다. idempotencyKey={}",
                                    request.getIdempotencyKey(), recordException);
                        }
                    }
                });
    }

    /** 운영자가 원인별로 검색할 수 있도록 Mock 실패 상황을 짧은 코드로 분류한다. */
    private String determineFailureCode(RuntimeException exception) {
        String message = exception.getMessage();
        if (message != null && message.contains("잔액이 이체 금액보다 부족")) {
            return "INSUFFICIENT_BALANCE";
        }
        if (message != null && message.contains("계좌를 찾을 수 없")) {
            return "ACCOUNT_NOT_FOUND";
        }
        return "MOCK_TRANSFER_ERROR";
    }

    /** DB 접근 전에 필수값을 검증하여 불완전한 PENDING 요청이 생성되지 않게 한다. */
    private void validateCommand(KbTransferCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("KB 송금 요청 정보가 필요합니다.");
        }
        if (isBlank(command.getIdempotencyKey())) {
            throw new IllegalArgumentException("KB 송금 멱등성 키는 필수입니다.");
        }
        if (isBlank(command.getSourceKbAccountId())
                || isBlank(command.getDestinationKbAccountId())) {
            throw new IllegalArgumentException("이체할 출금·입금 계좌 ID는 필수입니다.");
        }
        if (command.getSourceKbAccountId().trim()
                .equals(command.getDestinationKbAccountId().trim())) {
            throw new IllegalArgumentException("출금 계좌와 입금 계좌는 서로 달라야 합니다.");
        }
        if (command.getAmount() == null || command.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("이체 금액은 0보다 커야 합니다.");
        }
        if (command.getTransactionCategory() == null) {
            throw new IllegalArgumentException("송금 거래 분류는 필수입니다.");
        }
        /*
         * 정산 요청은 이후 그룹장이 본인 책임의 실패 목록을 조회할 수 있어야 하므로
         * 그룹장의 group_user_id와 실제 수령자의 user_id가 모두 필요하다. 예치 요청은
         * 정산 수령자가 존재하지 않으므로 settlementReceiverId가 NULL이어도 정상이다.
         */
        if (command.getTransactionCategory() == TransactionCategory.SETTLEMENT) {
            if (command.getGroupUserId() == null) {
                throw new IllegalArgumentException("정산 송금의 그룹장 참여 ID는 필수입니다.");
            }
            if (isBlank(command.getSettlementReceiverId())) {
                throw new IllegalArgumentException("정산 송금의 수령자 ID는 필수입니다.");
            }
            if (command.getSettlementReceiverId().trim().length() > 50) {
                throw new IllegalArgumentException("정산 송금의 수령자 ID는 50자 이하여야 합니다.");
            }
            if (command.getRoundId() == null) {
                throw new IllegalArgumentException("정산 송금의 라운드 ID는 필수입니다.");
            }
        }
    }

    /** 같은 키를 다른 계좌나 금액에 재사용하는 잘못된 요청을 명확히 거부한다. */
    private void validateSameRequest(KbTransferRequestVO existing,
                                     KbTransferCommand command) {
        String normalizedSettlementReceiverId = command.getSettlementReceiverId() == null
                ? null : command.getSettlementReceiverId().trim();
        boolean sameRequest = existing.getSourceKbAccountId()
                .equals(command.getSourceKbAccountId().trim())
                && existing.getDestinationKbAccountId()
                .equals(command.getDestinationKbAccountId().trim())
                && existing.getAmount().compareTo(command.getAmount()) == 0
                && existing.getTransactionCategory() == command.getTransactionCategory()
                && Objects.equals(existing.getGroupUserId(), command.getGroupUserId())
                && Objects.equals(existing.getSettlementReceiverId(),
                normalizedSettlementReceiverId)
                && Objects.equals(existing.getRoundId(), command.getRoundId());
        if (!sameRequest) {
            throw new IllegalArgumentException("동일한 멱등성 키가 다른 송금 요청에 사용되었습니다.");
        }
    }

    /**
     * 기존 요청의 상태에 따라 재사용 가능한 성공 결과를 반환하거나 재송금을 중단한다.
     * UNKNOWN을 실패처럼 곧바로 재실행하지 않는 것이 중복 송금 방지의 핵심이다.
     */
    private KbTransferResult resultFromExisting(KbTransferRequestVO existing) {
        if (existing.getTransferStatus() == KbTransferStatus.SUCCESS) {
            if (existing.getSourceBalanceAfter() == null
                    || existing.getDestinationBalanceAfter() == null) {
                throw new IllegalStateException("기존 KB 송금 성공 요청의 잔액 결과가 없습니다.");
            }
            return new KbTransferResult(
                    existing.getKbTransferRequestId(),
                    existing.getKbTransactionId(),
                    existing.getSourceBalanceAfter(),
                    existing.getDestinationBalanceAfter());
        }
        if (existing.getTransferStatus() == KbTransferStatus.FAILED) {
            throw new IllegalStateException("기존 KB 송금 요청이 실패 상태입니다: "
                    + nullToEmpty(existing.getFailureMessage()));
        }
        if (existing.getTransferStatus() == KbTransferStatus.UNKNOWN) {
            throw new IllegalStateException("기존 KB 송금의 처리 결과가 불명확하므로 거래조회가 필요합니다.");
        }
        throw new IllegalStateException("기존 KB 송금 요청이 아직 처리 중입니다.");
    }

    /** 존재하지 않는 계좌와 0원 계좌를 구분하지 않고 null 여부로 안전하게 검사한다. */
    private BigDecimal requireBalance(String kbAccountId, String accountRole) {
        BigDecimal balance = kbTransferMapper.findKbAccountBalanceForUpdate(kbAccountId);
        if (balance == null) {
            throw new IllegalStateException(accountRole + " KB 계좌를 찾을 수 없습니다.");
        }
        return balance;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
