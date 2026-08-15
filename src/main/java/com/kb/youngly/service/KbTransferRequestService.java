package com.kb.youngly.service;

import com.kb.youngly.dto.transfer.KbTransferCommand;
import com.kb.youngly.enums.KbTransferStatus;
import com.kb.youngly.enums.TransactionCategory;
import com.kb.youngly.mapper.KbTransferMapper;
import com.kb.youngly.vo.transfer.KbTransferRequestVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 외부 KB 송금을 실행하기 전에 송금 요청을 영속화하는 서비스.
 *
 * <p>송금을 실행하는 서비스와 분리한 핵심 이유는 PENDING 요청을 반드시 외부 호출보다 먼저
 * 커밋하기 위해서다. 외부 호출 도중 서버가 종료되거나 응답이 유실되더라도 커밋된 PENDING
 * 행이 남으므로, 멱등성 키를 사용해 KB 거래 결과를 조회하고 후속 복구를 수행할 수 있다.</p>
 */
@Service
@RequiredArgsConstructor
public class KbTransferRequestService {

    private final KbTransferMapper kbTransferMapper;

    /**
     * 송금 명령을 PENDING 요청으로 생성하고 독립적으로 커밋한다.
     *
     * <p>호출자인 예치·정산 서비스에 이미 트랜잭션이 있더라도 REQUIRES_NEW로 분리한다.
     * 따라서 이후 송금이나 Youngly 업무 반영이 롤백돼도, 외부 송금을 시도하기 전에 만든
     * 요청 기록은 사라지지 않는다.</p>
     *
     * <p>동일 멱등성 키의 요청이 이미 존재하면 새 행을 만들지 않고 기존 요청을 반환한다.
     * 계좌·금액 등 세부 내용이 같은지는 실제 실행 경계인 KbTransferService가 다시 검증하여
     * 동일 키를 다른 송금에 재사용하는 것을 차단한다.</p>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public KbTransferRequestVO createPending(KbTransferCommand command) {
        validateCommand(command);

        KbTransferRequestVO existing = kbTransferMapper.findByIdempotencyKeyForUpdate(
                command.getIdempotencyKey().trim());
        if (existing != null) {
            return existing;
        }

        KbTransferRequestVO request = KbTransferRequestVO.builder()
                .idempotencyKey(command.getIdempotencyKey().trim())
                .sourceKbAccountId(command.getSourceKbAccountId().trim())
                .destinationKbAccountId(command.getDestinationKbAccountId().trim())
                .amount(command.getAmount())
                .transactionCategory(command.getTransactionCategory())
                .transferStatus(KbTransferStatus.PENDING)
                .groupUserId(command.getGroupUserId())
                .settlementReceiverId(command.getSettlementReceiverId() == null
                        ? null : command.getSettlementReceiverId().trim())
                .roundId(command.getRoundId())
                .build();

        if (kbTransferMapper.insertPending(request) != 1
                || request.getKbTransferRequestId() == null) {
            throw new IllegalStateException("KB 송금 PENDING 요청 생성에 실패했습니다.");
        }
        return request;
    }

    /** 불완전한 명령이 영구적인 PENDING 데이터로 남지 않도록 INSERT 전에 검증한다. */
    private void validateCommand(KbTransferCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("KB 송금 요청 정보가 필요합니다.");
        }
        if (isBlank(command.getIdempotencyKey())) {
            throw new IllegalArgumentException("KB 송금 멱등성 키는 필수입니다.");
        }
        if (command.getIdempotencyKey().trim().length() > 100) {
            throw new IllegalArgumentException("KB 송금 멱등성 키는 100자 이하여야 합니다.");
        }
        if (isBlank(command.getSourceKbAccountId())
                || isBlank(command.getDestinationKbAccountId())) {
            throw new IllegalArgumentException("이체할 출금·입금 계좌 ID는 필수입니다.");
        }
        if (command.getSourceKbAccountId().trim().length() > 50
                || command.getDestinationKbAccountId().trim().length() > 50) {
            throw new IllegalArgumentException("KB 계좌 ID는 50자 이하여야 합니다.");
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
