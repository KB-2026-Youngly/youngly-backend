package com.kb.youngly.service;

import com.kb.youngly.dto.transfer.KbTransferCommand;
import com.kb.youngly.dto.transfer.KbTransferRequestResponse;
import com.kb.youngly.mapper.KbTransferMapper;
import com.kb.youngly.vo.transfer.KbTransferRequestVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 실패가 확정된 라운드 정산 송금 한 건을 기존 요청 행으로 다시 실행한다. */
@Service
@RequiredArgsConstructor
public class KbTransferRetryService {

    private final KbTransferRetryPreparationService retryPreparationService;
    private final KbTransferService kbTransferService;
    private final KbTransferMapper kbTransferMapper;

    /**
     * 그룹장이 소유한 FAILED 정산 요청을 재실행하고 갱신된 기존 요청을 반환한다.
     *
     * <p>새 {@code kb_transfer_requests} 행이나 새 멱등성 키를 만들지 않는다. 준비 서비스가
     * 기존 행을 PENDING으로 바꾼 다음, 최초 요청에 저장된 계좌·금액·멱등성 키를 그대로
     * 사용한다. 따라서 클라이언트가 재시도 금액이나 계좌를 변조할 수 없다.</p>
     */
    @Transactional
    public KbTransferRequestResponse retry(
            String userId,
            Long roundId,
            Long kbTransferRequestId
    ) {
        KbTransferRequestVO request = retryPreparationService.prepare(
                userId, roundId, kbTransferRequestId);

        KbTransferCommand command = KbTransferCommand.builder()
                .idempotencyKey(request.getIdempotencyKey())
                .sourceKbAccountId(request.getSourceKbAccountId())
                .destinationKbAccountId(request.getDestinationKbAccountId())
                .amount(request.getAmount())
                .transactionCategory(request.getTransactionCategory())
                .groupUserId(request.getGroupUserId())
                .settlementReceiverId(request.getSettlementReceiverId())
                .roundId(request.getRoundId())
                .build();

        // 성공 시 기존 송금 서비스의 markSuccess가 동일 PK 행을 SUCCESS로 갱신한다.
        kbTransferService.transfer(command);

        KbTransferRequestVO updated = kbTransferMapper.findById(kbTransferRequestId);
        if (updated == null) {
            throw new IllegalStateException("재시도한 정산 요청 결과를 찾을 수 없습니다.");
        }
        return toResponse(updated);
    }

    private KbTransferRequestResponse toResponse(KbTransferRequestVO request) {
        return KbTransferRequestResponse.builder()
                .transferRequestId(request.getKbTransferRequestId())
                .amount(request.getAmount())
                .transactionCategory(request.getTransactionCategory())
                .transferStatus(request.getTransferStatus())
                .kbTransactionId(request.getKbTransactionId())
                .failureCode(request.getFailureCode())
                .failureMessage(request.getFailureMessage())
                .settlementReceiverId(request.getSettlementReceiverId())
                .roundId(request.getRoundId())
                .requestedAt(request.getRequestedAt())
                .completedAt(request.getCompletedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}
