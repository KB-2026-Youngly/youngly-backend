package com.kb.youngly.controller;

import com.kb.youngly.dto.round.RoundRankingResponse;
import com.kb.youngly.dto.round.RoundResponse;
import com.kb.youngly.dto.round.RoundSettlementResponse;
import com.kb.youngly.dto.round.RoundUserResponse;
import com.kb.youngly.dto.transfer.KbTransferRequestResponse;
import com.kb.youngly.service.RoundQueryService;
import com.kb.youngly.service.RoundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rounds")
@RequiredArgsConstructor
public class RoundQueryController {

    private final RoundService roundService;
    private final RoundQueryService roundQueryService;

    // 라운드 조회
    @GetMapping("/{roundId}")
    public ResponseEntity<RoundResponse> getRound(
            @PathVariable Long roundId) {

        return ResponseEntity.ok(
                roundService.getRound(roundId)
        );
    }

    // 라운드 참여자 조회
    @GetMapping("/{roundId}/users")
    public ResponseEntity<List<RoundUserResponse>> getRoundUsers(
            @PathVariable Long roundId) {

        return ResponseEntity.ok(
                roundService.getRoundUsers(roundId)
        );
    }

    // 라운드별 사용자 랭킹 조회
    @GetMapping("/{roundId}/ranking")
    public ResponseEntity<List<RoundRankingResponse>> getRanking(
            @AuthenticationPrincipal String userId,
            @PathVariable Long roundId
    ) {
        return ResponseEntity.ok(
                roundQueryService.getRoundRanking(userId, roundId)
        );
    }

    // 라운드별 정산 결과 조회
    @GetMapping("/{roundId}/settlements")
    public ResponseEntity<List<RoundSettlementResponse>> getSettlements(
            @AuthenticationPrincipal String userId,
            @PathVariable Long roundId
    ) {
        return ResponseEntity.ok(
                roundQueryService.getRoundSettlements(userId, roundId)
        );
    }

    /**
     * 특정 라운드의 정산 송금 요청 내역을 조회한다.
     *
     * <p>groupUserId는 요청 파라미터로 받지 않는다. JWT 인증 결과인 userId를 사용해
     * 서버가 해당 라운드 그룹의 group_user_id를 확인하므로, 호출자가 다른 사용자의
     * 참여 ID를 임의로 지정해 정산 요청을 조회할 수 없다.</p>
     */
    @GetMapping("/{roundId}/transfer-requests")
    public ResponseEntity<List<KbTransferRequestResponse>> getTransferRequests(
            Authentication authentication,
            @PathVariable Long roundId
    ) {
        // JWT 필터가 Authentication.name에 저장한 user_id만 신뢰한다.
        String userId = authentication.getName();
        return ResponseEntity.ok(
                roundQueryService.getRoundTransferRequests(userId, roundId)
        );
    }
}
