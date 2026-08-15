package com.kb.youngly.service.impl;

import com.kb.youngly.dto.round.RoundRankingResponse;
import com.kb.youngly.dto.round.RoundSettlementResponse;
import com.kb.youngly.dto.transfer.KbTransferRequestResponse;
import com.kb.youngly.mapper.KbTransferMapper;
import com.kb.youngly.mapper.RoundMapper;
import com.kb.youngly.service.RoundQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoundQueryServiceImpl implements RoundQueryService {

    private final RoundMapper roundMapper;
    private final KbTransferMapper kbTransferMapper;
    // 라운드별 사용자 랭킹 조회
    @Override
    public List<RoundRankingResponse> getRoundRanking(
            String userId,
            Long roundId
    ) {
        validateRoundExists(roundId);

        return roundMapper.findRoundRanking(roundId);
    }
    // 라운드별 정산 결과 조회
    @Override
    public List<RoundSettlementResponse> getRoundSettlements(
            String userId,
            Long roundId
    ) {
        validateRoundExists(roundId);

        return roundMapper.findRoundSettlements(roundId);
    }

    /**
     * 라운드 정산 과정에서 생성된 KB 송금 요청 중 로그인 사용자의 관리 대상만 반환한다.
     *
     * <p>클라이언트가 groupUserId를 보내도록 하면 다른 참여자의 값을 대입할 수 있으므로,
     * 인증 토큰에서 얻은 userId만 서비스에 전달하고 실제 group_user_id 비교는 Mapper의
     * 조인 조건에서 수행한다. 조건에 맞는 요청이 없거나 사용자가 해당 정산 요청의 관리
     * 주체가 아니면 빈 목록을 반환하여 다른 사용자의 요청 존재 여부도 노출하지 않는다.</p>
     */
    @Override
    public List<KbTransferRequestResponse> getRoundTransferRequests(
            String userId,
            Long roundId
    ) {
        validateAuthenticatedUser(userId);
        validateRoundExists(roundId);

        return kbTransferMapper.findSettlementRequestsByRoundAndUser(roundId, userId)
                .stream()
                .map(request -> KbTransferRequestResponse.builder()
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
                        .build())
                .toList();
    }

    /** 인증 정보가 비어 있으면 DB 조회 전에 명확한 권한 오류로 처리한다. */
    private void validateAuthenticatedUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }
    }

    // 라운드 존재 여부 확인
    private void validateRoundExists(Long roundId) {
        if (!roundMapper.existsRound(roundId)) {
            throw new IllegalArgumentException(
                    "존재하지 않는 라운드입니다."
            );
        }
    }
}
