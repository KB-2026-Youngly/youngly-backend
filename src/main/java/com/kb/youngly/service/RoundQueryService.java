package com.kb.youngly.service;

import com.kb.youngly.dto.round.RoundRankingResponse;
import com.kb.youngly.dto.round.RoundSettlementResponse;

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
}