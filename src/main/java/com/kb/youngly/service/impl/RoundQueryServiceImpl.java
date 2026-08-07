package com.kb.youngly.service.impl;

import com.kb.youngly.dto.round.RoundRankingResponse;
import com.kb.youngly.dto.round.RoundSettlementResponse;
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
    // 라운드별 사용자 랭킹 조회
    @Override
    public List<RoundRankingResponse> getRoundRanking(
            String userId,
            Long roundId
    ) {
        validateRoundAccess(userId, roundId);

        return roundMapper.findRoundRanking(roundId);
    }
    // 라운드별 정산 결과 조회
    @Override
    public List<RoundSettlementResponse> getRoundSettlements(
            String userId,
            Long roundId
    ) {
        validateRoundAccess(userId, roundId);

        return roundMapper.findRoundSettlements(roundId);
    }
    // 라운드 존재 여부 확인
    private void validateRoundAccess(
            String userId,
            Long roundId
    ) {
        if (!roundMapper.existsRound(roundId)) {
            throw new IllegalArgumentException(
                    "존재하지 않는 라운드입니다."
            );
        }
    }
}