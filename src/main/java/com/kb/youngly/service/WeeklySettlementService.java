package com.kb.youngly.service;

import com.kb.youngly.dto.round.WeeklySettlementTarget;
import com.kb.youngly.mapper.WeeklySettlementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 주간 챌린지 결산의 비즈니스 로직을 담당하는 서비스.
 *
 * <p>결산 기준일에 처리해야 하는 라운드를 조회하고, 각 라운드 참여자의
 * 승인 게시물 수가 그룹의 최소 인증 횟수 이상이면 다음 값을 함께 증가시킨다.</p>
 *
 * <ul>
 *     <li>{@code round_history.success_count}: 현재 라운드에서 성공한 주차 수</li>
 *     <li>{@code group_users.streak_count}: 해당 그룹에서 사용자가 연속 달성한 횟수</li>
 * </ul>
 *
 * <p>실제 집계와 갱신 SQL은 {@link WeeklySettlementMapper}에 위임한다.</p>
 */
@Service
@RequiredArgsConstructor
public class WeeklySettlementService {

    private final WeeklySettlementMapper weeklySettlementMapper;

    /**
     * 주어진 날짜에 주간 결산이 필요한 진행 중 라운드를 조회한다.
     *
     * <p>라운드 시작일로부터 6, 13, 20, 27...일째인 라운드가 대상이며,
     * 이 메서드는 조회만 수행하므로 읽기 전용 트랜잭션을 사용한다.</p>
     *
     * @param settlementDate 한국 시간 기준 결산일
     * @return 결산할 라운드와 주차별 집계 기간 목록
     */
    @Transactional(readOnly = true)
    public List<WeeklySettlementTarget> findDueSettlements(LocalDate settlementDate) {
        // 날짜가 없으면 대상 주차와 게시물 집계 범위를 계산할 수 없으므로 즉시 거부한다.
        if (settlementDate == null) {
            throw new IllegalArgumentException("결산 기준일은 필수입니다.");
        }
        return weeklySettlementMapper.findDueWeeklySettlements(settlementDate);
    }

    /**
     * 한 라운드의 한 주차를 결산한다.
     *
     * <p>주간 승인 게시물 수가 {@code groups.min_count} 이상인 참여자에 대해
     * 라운드 성공 횟수와 그룹 streak를 각각 1 증가시킨다. 두 갱신은 같은
     * 트랜잭션에서 실행되므로 어느 한쪽이 실패하면 전체 작업이 롤백된다.</p>
     *
     * <p>카운트를 변경하기 전에 참여자별 승인 게시물 수를 주간 결산 결과로 저장한다.
     * 동일한 라운드, 주차, 사용자의 결과가 이미 존재하면 갱신을 생략하여
     * 중복 카운트를 방지한다.</p>
     *
     * @param target 라운드, 그룹, 주차, 집계 기간 및 최소 인증 횟수 정보
     * @return 주간 목표를 달성하여 카운트가 증가한 참여자 수
     */
    @Transactional
    public int settle(WeeklySettlementTarget target) {
        // 잘못된 기간이나 식별자로 갱신 쿼리가 실행되지 않도록 먼저 검증한다.
        validateTarget(target);

        // 참여자별 승인 게시물 수를 먼저 저장해 결산 시점의 결과를 확정한다.
        int insertedResultCount = weeklySettlementMapper.insertWeeklySettlementResults(target);

        // 모든 참여자의 주간 결과가 이미 저장되어 있으면 중복 결산이므로 종료한다.
        if (insertedResultCount == 0) {
            return 0;
        }

        // 저장된 approved_post_count가 min_count 이상인 참여자의 두 카운트를 갱신한다.
        int successCount = weeklySettlementMapper.incrementRoundHistorySuccessCount(target);
        int streakCount = weeklySettlementMapper.incrementGroupUserStreakCount(target);

        // 갱신 인원이 다르면 일부 데이터만 변경된 상태이므로 예외를 발생시켜 모두 롤백한다.
        if (successCount != streakCount) {
            throw new IllegalStateException("주간 결산 대상자의 성공 횟수와 streak 반영 결과가 일치하지 않습니다.");
        }
        return successCount;
    }

    /** 결산 쿼리 실행에 필요한 필수값과 날짜 범위의 유효성을 검사한다. */
    private void validateTarget(WeeklySettlementTarget target) {
        // 식별자, 주차, 집계 기간, 최소 인증 횟수는 결산 대상과 성공 여부 계산에 모두 필요하다.
        if (target == null
                || target.getRoundId() == null
                || target.getGroupId() == null
                || target.getGroupId().trim().isEmpty()
                || target.getWeekNo() == null
                || target.getWeekNo() <= 0
                || target.getWeekStartDate() == null
                || target.getWeekEndDate() == null
                || target.getMinCount() == null
                || target.getMinCount() <= 0) {
            throw new IllegalArgumentException("주간 결산 대상 정보가 올바르지 않습니다.");
        }

        // 시작일과 종료일이 뒤집히면 승인 게시물 집계 범위가 성립하지 않는다.
        if (target.getWeekStartDate().isAfter(target.getWeekEndDate())) {
            throw new IllegalArgumentException("주간 결산 시작일은 종료일보다 늦을 수 없습니다.");
        }
    }
}
