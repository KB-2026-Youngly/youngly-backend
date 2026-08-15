package com.kb.youngly.service;

import com.kb.youngly.enums.KbTransferStatus;
import com.kb.youngly.mapper.KbTransferMapper;
import com.kb.youngly.vo.transfer.KbTransferRequestVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 실패한 정산 송금 요청을 재시도 가능한 상태로 준비하는 트랜잭션 경계.
 *
 * <p>별도 Bean의 {@code REQUIRES_NEW} 메서드로 분리한 이유는 기존 FAILED 행을 PENDING으로
 * 바꾼 결과를 실제 이체보다 먼저 커밋하기 위해서다. 이체가 실패하면 기존 실패 기록기가
 * 같은 행을 다시 FAILED로 갱신하며, 성공하면 기존 송금 서비스가 같은 행을 SUCCESS로 바꾼다.</p>
 */
@Service
@RequiredArgsConstructor
public class KbTransferRetryPreparationService {

    private final KbTransferMapper kbTransferMapper;

    /** 그룹장 소유권과 FAILED 상태를 잠금 상태에서 확인한 뒤 기존 행을 PENDING으로 전환한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public KbTransferRequestVO prepare(
            String userId,
            Long roundId,
            Long kbTransferRequestId
    ) {
        validateParameters(userId, roundId, kbTransferRequestId);

        KbTransferRequestVO request =
                kbTransferMapper.findOwnedSettlementRequestForUpdate(
                        kbTransferRequestId, roundId, userId);

        // 존재 여부와 권한 실패를 같은 응답으로 처리해 다른 사용자의 송금 요청 정보를 숨긴다.
        if (request == null) {
            throw new AccessDeniedException("해당 정산 요청을 재시도할 권한이 없습니다.");
        }
        if (request.getTransferStatus() != KbTransferStatus.FAILED) {
            throw new IllegalArgumentException("FAILED 상태의 정산 요청만 재시도할 수 있습니다.");
        }
        if (kbTransferMapper.markFailedRequestPending(kbTransferRequestId) != 1) {
            throw new IllegalStateException("정산 요청을 재시도 상태로 변경하지 못했습니다.");
        }

        return request;
    }

    private void validateParameters(String userId, Long roundId, Long kbTransferRequestId) {
        if (userId == null || userId.isBlank()) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }
        if (roundId == null || roundId <= 0) {
            throw new IllegalArgumentException("라운드 ID는 1 이상이어야 합니다.");
        }
        if (kbTransferRequestId == null || kbTransferRequestId <= 0) {
            throw new IllegalArgumentException("정산 요청 ID는 1 이상이어야 합니다.");
        }
    }
}
