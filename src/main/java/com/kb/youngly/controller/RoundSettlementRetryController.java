package com.kb.youngly.controller;

import com.kb.youngly.dto.transfer.KbTransferRequestResponse;
import com.kb.youngly.service.KbTransferRetryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 실패한 라운드 정산 송금 한 건을 그룹장이 수동으로 재시도하는 API. */
@RestController
@RequestMapping("/api/rounds/{roundId}/transfer-requests")
@RequiredArgsConstructor
public class RoundSettlementRetryController {

    private final KbTransferRetryService kbTransferRetryService;

    /**
     * 기존 FAILED 정산 요청을 같은 요청 ID와 멱등성 키로 다시 실행한다.
     * 요청 본문에서 금액이나 계좌를 받지 않으며, DB에 최초 저장된 송금 정보만 사용한다.
     */
    @PostMapping("/{transferRequestId}/retry")
    public ResponseEntity<KbTransferRequestResponse> retry(
            Authentication authentication,
            @PathVariable Long roundId,
            @PathVariable Long transferRequestId
    ) {
        String userId = authentication.getName();
        return ResponseEntity.ok(
                kbTransferRetryService.retry(userId, roundId, transferRequestId)
        );
    }
}
