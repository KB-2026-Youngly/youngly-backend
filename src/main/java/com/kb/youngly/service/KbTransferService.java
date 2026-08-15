package com.kb.youngly.service;

import com.kb.youngly.dto.transfer.KbTransferCommand;
import com.kb.youngly.dto.transfer.KbTransferResult;

/** KB 계좌 간 송금 경계. 현재는 DB 기반 Mock 구현을 사용하고 향후 실제 API 구현으로 교체한다. */
public interface KbTransferService {

    /**
     * 사전에 생성·커밋된 PENDING 요청 한 건을 실행한다.
     *
     * <p>호출자는 반드시 같은 명령으로 {@link KbTransferRequestService#createPending(KbTransferCommand)}을
     * 먼저 호출해야 한다. 이 메서드는 요청 행을 생성하지 않으며, PENDING이 없으면 외부 송금을
     * 시작하지 않는다.</p>
     *
     * @return 성공이 확정된 송금 결과
     * @throws IllegalArgumentException 요청 형식 또는 멱등성 키 재사용 내용이 올바르지 않은 경우
     * @throws IllegalStateException 잔액 부족, 계좌 부재 또는 처리 상태를 확정할 수 없는 경우
     */
    KbTransferResult transfer(KbTransferCommand command);
}
