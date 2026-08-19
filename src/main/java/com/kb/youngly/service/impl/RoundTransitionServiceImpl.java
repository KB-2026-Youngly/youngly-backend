package com.kb.youngly.service.impl;

import com.kb.youngly.dto.round.RoundParticipantDTO;
import com.kb.youngly.enums.GroupStatus;
import com.kb.youngly.enums.RoundStatus;
import com.kb.youngly.mapper.RoundMapper;
import com.kb.youngly.service.RoundTransitionService;
import com.kb.youngly.vo.group.GroupVO;
import com.kb.youngly.vo.round.RoundHistoryVO;
import com.kb.youngly.vo.round.RoundVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 라운드 종료 상태 변경과 다음 라운드 생성을 담당하는 서비스 구현체.
 *
 * <p>전환 대상 그룹 조회는 읽기 전용 트랜잭션으로 수행하고, 그룹별 전환은
 * 종료 라운드 상태 변경, 다음 라운드 생성, 참여 이력 생성을 하나의 쓰기
 * 트랜잭션으로 처리한다.</p>
 *
 * <p>그룹 행에 배타적 잠금을 적용하여 스케줄러 중복 실행이나 수동 라운드 생성이
 * 동시에 발생하더라도 동일한 회차가 중복 생성되지 않도록 한다.</p>
 */
@Service
@RequiredArgsConstructor
public class RoundTransitionServiceImpl implements RoundTransitionService {

    /** 라운드, 그룹 및 라운드 참여 이력 데이터에 접근하는 Mapper. */
    private final RoundMapper roundMapper;

    /**
     * 기준일에 종료되는 진행 중 라운드를 가진 진행 중 그룹을 조회한다.
     *
     * @param transitionDate 한국시간 기준 라운드 전환일
     * @return 자동 전환 대상 그룹 ID 목록
     */
    @Override
    @Transactional(readOnly = true)
    public List<String> findDueGroupIds(LocalDate transitionDate) {
        // 날짜가 없으면 종료일 비교 조건을 만들 수 없으므로 조회 전에 거부한다.
        if (transitionDate == null) {
            throw new IllegalArgumentException("라운드 전환 기준일은 필수입니다.");
        }

        // 그룹과 라운드가 모두 ONGOING이고 종료일이 기준일인 그룹만 반환한다.
        return roundMapper.findDueRoundGroupIds(transitionDate);
    }

    /**
     * 한 그룹의 종료 라운드를 정산 대기로 전환하고 다음 라운드를 생성한다.
     *
     * <p>다음 라운드는 이전 라운드의 종료일 다음 날 시작하며, 시작일을 첫날로 포함하여
     * 정확히 그룹의 {@code roundCycleDays}일 동안 진행되도록 종료일을 계산한다.
     * 회차 번호는 이전 회차보다 1 증가하고 상태는 {@code ONGOING}으로 저장한다.</p>
     *
     * <p>그룹 행을 잠근 뒤 대상 라운드를 다시 확인하므로 스케줄러가 중복 실행되더라도
     * 동일한 다음 회차가 두 번 생성되지 않는다. 이미 처리됐거나 전환 대상이 아닌
     * 그룹은 데이터 변경 없이 {@code false}를 반환한다.</p>
     *
     * @param groupId 전환할 라운드가 속한 그룹 ID
     * @param transitionDate 한국시간 기준 라운드 전환일
     * @return 상태 전환이 수행되었으면 {@code true}, 처리할 대상이 없으면 {@code false}
     */
    @Override
    @Transactional
    public boolean transitionToNextRound(String groupId, LocalDate transitionDate) {
        // 잘못된 식별자나 날짜로 잠금 및 갱신 쿼리가 실행되지 않게 먼저 검증한다.
        if (groupId == null || groupId.trim().isEmpty() || transitionDate == null) {
            throw new IllegalArgumentException("라운드 전환 대상 정보가 올바르지 않습니다.");
        }

        // 그룹 단위 잠금으로 수동 생성과 자동 생성 요청의 회차 번호 계산을 직렬화한다.
        GroupVO group = roundMapper.findGroupForUpdate(groupId.trim());
        // 조회 이후 그룹이 삭제되거나 진행 종료된 경우에는 더 이상 라운드를 만들지 않는다.
        if (group == null || group.getGroupStatus() != GroupStatus.ONGOING) {
            return false;
        }

        // 다음 라운드의 종료일과 참여 이력을 생성하는 데 필요한 그룹 설정을 검증한다.
        if (group.getRoundCycleDays() == null || group.getRoundCycleDays() <= 0) {
            throw new IllegalStateException("그룹의 라운드 주기가 올바르지 않습니다.");
        }
        if (group.getDefaultFailPassCount() != null && group.getDefaultFailPassCount() < 0) {
            throw new IllegalStateException("그룹의 실패 패스 기본 횟수가 올바르지 않습니다.");
        }
        if (group.getMoimAccountId() == null || group.getMoimAccountId().trim().isEmpty()) {
            throw new IllegalStateException("그룹에 연결된 모임통장이 없습니다.");
        }

        // 잠금을 획득한 뒤 대상 상태를 재조회하여 이미 처리된 그룹은 건너뛴다.
        RoundVO previousRound =
                roundMapper.findOngoingRoundEndingOn(groupId.trim(), transitionDate);
        if (previousRound == null) {
            return false;
        }

        // 이전 라운드의 상태를 정산 대기로 변경
        // 종료 라운드의 상태 변경이 성공한 경우에만 다음 회차 생성을 진행한다.
        if (roundMapper.markRoundWaitingSettlement(previousRound.getRoundId(), transitionDate) != 1) {
            return false;
        }

        // 수동 요청 등으로 다음 회차가 이미 만들어졌다면 종료 상태만 반영하고 중복 생성하지 않는다.
        RoundVO latestRound = roundMapper.findLatestRound(groupId.trim());
        if (latestRound != null && latestRound.getRoundNo() > previousRound.getRoundNo()) {
            return true;
        }

        // 새 라운드는 이전 라운드 종료일의 다음 날부터 공백 없이 시작한다.
        LocalDate nextStartDate = previousRound.getEndDate().plusDays(1);

        // 시작일을 첫날로 포함하여 정확히 roundCycleDays일이 되도록 종료일을 계산한다.
        RoundVO nextRound = RoundVO.builder()
                .groupId(groupId.trim())
                .roundNo(previousRound.getRoundNo() + 1)
                .startDate(nextStartDate)
                .endDate(nextStartDate.plusDays(group.getRoundCycleDays() - 1L))
                .roundStatus(RoundStatus.ONGOING)
                .build();

        // INSERT 행 수와 자동 생성 PK를 모두 확인해 불완전한 저장을 롤백 대상으로 만든다.
        if (roundMapper.insertRound(nextRound) != 1 || nextRound.getRoundId() == null) {
            throw new IllegalStateException("다음 라운드 생성에 실패했습니다.");
        }

        // 새 라운드 시작 시점의 참여자와 수령 계좌를 round_history에 고정한다.
        List<RoundParticipantDTO> participants = roundMapper.findRoundParticipants(groupId.trim());
        int defaultFailPassCount = group.getDefaultFailPassCount() == null
                ? 0
                : group.getDefaultFailPassCount();
        for (RoundParticipantDTO participant : participants) {
            if (participant.getAccountId() == null || participant.getAccountId().trim().isEmpty()) {
                throw new IllegalStateException(
                        participant.getUserId() + " 사용자의 미래 적립금 수령 계좌가 없습니다."
                );
            }

            RoundHistoryVO history = RoundHistoryVO.builder()
                    .roundId(nextRound.getRoundId())
                    .userId(participant.getUserId())
                    .accountId(participant.getAccountId())
                    .moimAccountId(group.getMoimAccountId())
                    .successCount(0)
                    .remainingFailPassCount(defaultFailPassCount)
                    .build();

            // 참여자 한 명이라도 이력이 저장되지 않으면 라운드 생성과 상태 변경도 함께 롤백한다.
            if (roundMapper.insertRoundHistory(history) != 1) {
                throw new IllegalStateException("다음 라운드 참여 이력 생성에 실패했습니다.");
            }
        }
        return true;
    }
}
