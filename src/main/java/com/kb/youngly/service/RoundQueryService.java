package com.kb.youngly.service;

import com.kb.youngly.dto.round.RoundRankingResponse;
import com.kb.youngly.dto.round.RoundSettlementResponse;
import com.kb.youngly.dto.transfer.KbTransferRequestResponse;

import java.util.List;

public interface RoundQueryService {

    List<RoundRankingResponse> getRoundRanking(
            String userId,
            Long roundId
    );

    List<RoundSettlementResponse> getRoundSettlements(
            String userId,
            Long roundId
    );

    /** 로그인 사용자가 관리하는 특정 라운드의 정산 송금 요청 내역을 조회한다. */
    List<KbTransferRequestResponse> getRoundTransferRequests(
            String userId,
            Long roundId
    );
}
