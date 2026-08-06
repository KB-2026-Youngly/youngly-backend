package com.kb.youngly.service.impl;

import com.kb.youngly.dto.round.CreateRoundRequest;
import com.kb.youngly.dto.round.CreateRoundResponse;
import com.kb.youngly.dto.round.RoundParticipantDTO;
import com.kb.youngly.dto.round.RoundResponse;
import com.kb.youngly.enums.RoundStatus;
import com.kb.youngly.mapper.RoundMapper;
import com.kb.youngly.service.RoundService;
import com.kb.youngly.vo.group.GroupVO;
import com.kb.youngly.vo.round.RoundHistoryVO;
import com.kb.youngly.vo.round.RoundVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 라운드 생성 서비스 구현체.
 *
 * <p>그룹별 회차 번호 계산, 종료일 계산, 라운드 저장 및 참여자별
 * round_history 초기화를 하나의 트랜잭션에서 수행한다.</p>
 */
@Service
@RequiredArgsConstructor
public class RoundServiceImpl implements RoundService {

    private final RoundMapper roundMapper;

    /**
     * 첫 라운드는 그룹장이 지정한 시작일로 생성하고, 이후 라운드는
     * 직전 라운드 종료일의 다음 날부터 자동으로 시작한다.
     * 이력이 하나라도 저장되지 않거나 참여자의 수령 계좌가 없으면
     * 예외를 발생시켜 라운드 저장까지 모두 롤백한다.
     */
    @Override
    @Transactional
    public CreateRoundResponse createRound(String userId, String groupId, CreateRoundRequest request) {
        String normalizedUserId = requireText(userId, "인증된 사용자 정보가 없습니다.");
        String normalizedGroupId = requireText(groupId, "그룹 ID는 필수입니다.");

        // 그룹 행 잠금 이후 직전 라운드를 조회해 같은 그룹의 동시 생성 요청을 직렬화한다.
        GroupVO group = roundMapper.findGroupForUpdate(normalizedGroupId);
        if (group == null) {
            throw new IllegalArgumentException("그룹을 찾을 수 없습니다.");
        }
        if (!normalizedUserId.equals(group.getUserId())) {
            throw new IllegalArgumentException("그룹장만 라운드를 생성할 수 있습니다.");
        }
        if (group.getRoundCycleDays() == null || group.getRoundCycleDays() <= 0) {
            throw new IllegalStateException("그룹의 라운드 주기가 올바르지 않습니다.");
        }
        if (group.getMoimAccountId() == null || group.getMoimAccountId().trim().isEmpty()) {
            throw new IllegalStateException("그룹에 연결된 모임통장이 없습니다.");
        }

        RoundVO latestRound = roundMapper.findLatestRound(normalizedGroupId);
        boolean isFirstRound = latestRound == null;
        int roundNo = isFirstRound ? 1 : latestRound.getRoundNo() + 1;
        LocalDate startDate = resolveStartDate(request, latestRound);

        // 종료일과 상태는 서버에서 그룹 설정을 기준으로 확정한다.
        RoundVO round = RoundVO.builder()
                .groupId(normalizedGroupId)
                .roundNo(roundNo)
                .startDate(startDate)
                .endDate(startDate.plusDays(group.getRoundCycleDays()))
                .roundStatus(RoundStatus.ONGOING)
                .build();

        if (roundMapper.insertRound(round) != 1 || round.getRoundId() == null) {
            throw new IllegalStateException("라운드 생성에 실패했습니다.");
        }

        // 첫 라운드가 생성되면 모집 중인 그룹을 진행 중 상태로 전환한다.
        if (isFirstRound) {
            roundMapper.startRecruitingGroup(normalizedGroupId);
        }

        // 참여 대기 및 참여 중인 사용자만 새 라운드의 정산 대상 이력에 포함한다.
        List<RoundParticipantDTO> participants = roundMapper.findRoundParticipants(normalizedGroupId);
        for (RoundParticipantDTO participant : participants) {
            if (participant.getAccountId() == null || participant.getAccountId().trim().isEmpty()) {
                throw new IllegalStateException(
                        participant.getUserId() + " 사용자의 미래 적립금 수령 계좌가 없습니다."
                );
            }

            // 순위와 정산 금액은 라운드 종료 시 계산하므로 초기값에는 포함하지 않는다.
            RoundHistoryVO history = RoundHistoryVO.builder()
                    .roundId(round.getRoundId())
                    .userId(participant.getUserId())
                    .accountId(participant.getAccountId())
                    .moimAccountId(group.getMoimAccountId())
                    .successCount(0)
                    .remainingFailPassCount(0)
                    .build();
            if (roundMapper.insertRoundHistory(history) != 1) {
                throw new IllegalStateException("라운드 참여 이력 생성에 실패했습니다.");
            }
        }

        return CreateRoundResponse.builder()
                .roundId(round.getRoundId())
                .groupId(round.getGroupId())
                .roundNo(round.getRoundNo())
                .startDate(round.getStartDate())
                .endDate(round.getEndDate())
                .roundStatus(round.getRoundStatus())
                .participantCount(participants.size())
                .build();
    }

    /**
     * 첫 라운드는 요청 시작일을 사용하고, 이후 라운드는 직전 종료일 다음 날로 계산한다.
     */
    private LocalDate resolveStartDate(CreateRoundRequest request, RoundVO latestRound) {
        if (latestRound == null) {
            if (request == null || request.getStartDate() == null) {
                throw new IllegalArgumentException("첫 번째 라운드의 시작일은 필수입니다.");
            }
            return request.getStartDate();
        }
        if (latestRound.getEndDate() == null) {
            throw new IllegalStateException("직전 라운드의 종료일이 없습니다.");
        }
        return latestRound.getEndDate().plusDays(1);
    }

    /** 문자열 필수값을 검증하고 앞뒤 공백을 제거한다. */
    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    // 라운드 조회
    @Override
    @Transactional(readOnly = true)
    public RoundResponse getRound(Long roundId) {

        if (roundId == null) {
            throw new IllegalArgumentException("라운드 ID는 필수입니다.");
        }

        RoundVO round = roundMapper.findRoundById(roundId);

        if (round == null) {
            throw new IllegalArgumentException("라운드를 찾을 수 없습니다.");
        }

        return RoundResponse.builder()
                .roundId(round.getRoundId())
                .roundNo(round.getRoundNo())
                .startDate(round.getStartDate())
                .endDate(round.getEndDate())
                .roundStatus(round.getRoundStatus())
                .build();
    }
}
