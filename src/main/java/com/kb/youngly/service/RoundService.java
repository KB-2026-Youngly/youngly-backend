package com.kb.youngly.service;

import com.kb.youngly.dto.round.CreateRoundRequest;
import com.kb.youngly.dto.round.CreateRoundResponse;
import com.kb.youngly.dto.round.RoundResponse;
import com.kb.youngly.dto.round.RoundUserResponse;

import java.util.List;

/**
 * 그룹 챌린지의 라운드 생성 기능을 제공한다.
 *
 * <p>라운드 생성과 해당 라운드에 참여할 사용자별 이력 생성은
 * 하나의 작업 단위로 처리되어야 한다.</p>
 */
public interface RoundService {

    /**
     * 그룹에 다음 라운드를 생성하고 참여 대상자의 {@code round_history}를 초기화한다.
     *
     * @param userId  라운드를 생성하는 로그인 사용자 ID
     * @param groupId 라운드를 생성할 그룹 ID
     * @param request 첫 라운드라면 프론트엔드가 지정한 시작일, 이후 라운드라면 생략 가능
     * @return 생성된 라운드와 참여 인원 정보
     */
    CreateRoundResponse createRound(String userId, String groupId, CreateRoundRequest request);

    /**
     * 라운드 조회
     */
    RoundResponse getRound(Long roundId);

    /**
     * 라운드 참여자 조회
     */
    List<RoundUserResponse> getRoundUsers(Long roundId);
}
