package com.kb.youngly.service;

import com.kb.youngly.enums.KbTransferStatus;
import com.kb.youngly.mapper.KbTransferMapper;
import com.kb.youngly.vo.transfer.KbTransferRequestVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 송금 본 트랜잭션과 독립적으로 실패 또는 결과 불명확 상태를 저장하는 컴포넌트.
 *
 * <p>별도 Spring Bean으로 분리한 이유는 같은 클래스 안에서 자기 메서드를 호출하면
 * Spring 트랜잭션 프록시를 거치지 않아 {@code REQUIRES_NEW}가 적용되지 않기 때문이다.</p>
 */
@Service
@RequiredArgsConstructor
public class KbTransferFailureRecorder {

    private static final int MAX_FAILURE_MESSAGE_LENGTH = 255;

    private final KbTransferMapper kbTransferMapper;

    /**
     * 원래 트랜잭션이 끝난 이후 새로운 트랜잭션을 열어 실패 내역을 확정한다.
     *
     * <p>현재 Mock 구현은 로컬 DB 롤백으로 송금 미실행이 보장되므로 FAILED를 사용한다.
     * 실제 외부 API에서 타임아웃이 발생한 경우에는 송금 여부를 단정할 수 없으므로
     * 같은 메서드에 UNKNOWN을 전달한 뒤 거래조회로 최종 상태를 확인해야 한다.</p>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(KbTransferRequestVO request, KbTransferStatus status,
                       String failureCode, String failureMessage) {
        if (request == null || request.getIdempotencyKey() == null) {
            throw new IllegalArgumentException("기록할 KB 송금 요청 정보가 없습니다.");
        }
        if (status != KbTransferStatus.FAILED && status != KbTransferStatus.UNKNOWN) {
            throw new IllegalArgumentException("실패 기록은 FAILED 또는 UNKNOWN 상태만 허용합니다.");
        }

        /*
         * DB 컬럼 크기를 초과한 외부 오류 메시지 때문에 실패 기록 자체가 다시 실패하지 않도록
         * 최대 255자로 제한한다. 원본 예외 전체는 애플리케이션 로그에서 확인할 수 있다.
         */
        String normalizedMessage = failureMessage == null ? null : failureMessage.trim();
        if (normalizedMessage != null
                && normalizedMessage.length() > MAX_FAILURE_MESSAGE_LENGTH) {
            normalizedMessage = normalizedMessage.substring(0, MAX_FAILURE_MESSAGE_LENGTH);
        }

        request.setTransferStatus(status);
        request.setFailureCode(failureCode);
        request.setFailureMessage(normalizedMessage);
        // MySQL은 INSERT 시 1, 기존 행 UPDATE 시 2를 반환할 수 있으므로 1 이상이면 성공이다.
        if (kbTransferMapper.upsertFailure(request) < 1) {
            throw new IllegalStateException("KB 송금 실패 내역 저장에 실패했습니다.");
        }
    }
}
