package com.kb.youngly.service;

import com.kb.youngly.dto.transfer.KbTransferCommand;
import com.kb.youngly.dto.transfer.KbTransferResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 라운드 정산에서 참여자 한 명의 KB 송금을 독립된 트랜잭션으로 실행한다.
 *
 * <p>그룹 정산 트랜잭션에 송금을 직접 참여시키면 한 참여자의 예외가 전체 트랜잭션을
 * rollback-only 상태로 만들어 다음 참여자를 처리할 수 없다. REQUIRES_NEW 경계를 두면
 * 실패한 송금만 롤백되고, 호출자는 예외를 수집한 뒤 다음 참여자의 송금을 계속 시도할 수 있다.</p>
 *
 * <p>실제 KB API로 교체한 뒤에는 외부 송금 자체가 DB 트랜잭션과 독립적이므로, 이 클래스는
 * 요청 상태와 Youngly 내부 처리 경계를 분리하는 어댑터 역할을 하게 된다.</p>
 */
@Service
@RequiredArgsConstructor
public class KbTransferAttemptService {

    private final KbTransferService kbTransferService;

    /** 참가자 한 명의 사전 생성된 PENDING 요청을 별도 트랜잭션에서 처리한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public KbTransferResult transfer(KbTransferCommand command) {
        return kbTransferService.transfer(command);
    }
}
