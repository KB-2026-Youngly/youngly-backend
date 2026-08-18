package com.kb.youngly.scheduler;

import com.kb.youngly.dto.round.DailyBatchExecutionResponse;
import com.kb.youngly.dto.round.WeeklySettlementTarget;
import com.kb.youngly.mapper.PostMapper;
import com.kb.youngly.mapper.RoundMapper;
import com.kb.youngly.enums.NotificationType;
import com.kb.youngly.service.NotificationService;
import com.kb.youngly.service.RoundSettlementService;
import com.kb.youngly.service.RoundTransitionService;
import com.kb.youngly.service.WeeklySettlementService;
import com.kb.youngly.vo.round.RoundVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 자정에 실행되는 챌린지 일일 배치 작업을 순서대로 처리하는 통합 스케줄러.
 *
 * <p>서로 의존하는 작업이 같은 시각에 개별 {@code @Scheduled} 메서드로 실행되면
 * 호출 순서가 보장되지 않는다. 따라서 하나의 스케줄 진입점에서 다음 순서로
 * 각 작업을 직접 호출한다.</p>
 *
 * <ol>
 *     <li>시간이 만료된 PENDING 게시글 자동 승인</li>
 *     <li>자동 승인 결과를 포함한 주간 결산</li>
 *     <li>전날 종료된 라운드를 정산 대기로 전환하고 다음 라운드 생성</li>
 *     <li>이틀 전 종료된 정산 대기 라운드의 최종 정산</li>
 * </ol>
 *
 * <p>주간 결산, 라운드 전환 및 최종 정산의 트랜잭션 경계는 각 서비스 계층에
 * 유지된다. 따라서 한 대상의 실패가 이미 완료된 다른 대상의 트랜잭션까지
 * 롤백하지는 않는다.</p>
 */
@Component
@RequiredArgsConstructor
@Log4j2
public class DailyBatchScheduler {

    /** 서버 기본 시간대와 무관하게 한국 날짜를 사용한다. */
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    /** 전환 대상 조회와 그룹별 라운드 전환 트랜잭션을 수행하는 서비스. */
    private final RoundTransitionService roundTransitionService;

    /** 종료일 다음 날이 된 라운드의 계좌 이체와 정산 완료 처리를 수행하는 서비스. */
    private final RoundSettlementService roundSettlementService;

    /** 주간 승인 게시물 집계와 참여자 성공 횟수 반영을 수행하는 서비스. */
    private final WeeklySettlementService weeklySettlementService;

    /** 주간 결산 전에 시간이 만료된 게시물을 자동 승인하기 위한 Mapper. */
    private final PostMapper postMapper;

    private final NotificationService notificationService;
    private final RoundMapper roundMapper;

    /**
     * 매일 한국시간 00시 00분 00초에 일일 배치를 한 번 실행한다.
     *
     * <p>모든 단계가 동일한 {@code batchDate}를 기준으로 날짜를 계산하므로 작업 도중
     * 날짜가 바뀌거나 서버 기본 시간대가 달라도 대상 날짜가 달라지지 않는다.</p>
     */
    @Scheduled(cron = "0 25 11 * * *", zone = "Asia/Seoul")
    public void runDailyBatch() {
        executeDailyBatch(LocalDate.now(SERVICE_ZONE));
    }

    /**
     * 지정한 날짜를 기준으로 자동 스케줄과 동일한 일일 배치를 실행한다.
     *
     * <p>날짜를 인자로 분리하여 개발용 API에서도 실제 스케줄을 기다리지 않고
     * 동일한 순서와 날짜 계산을 재현할 수 있다.</p>
     *
     * @param batchDate 한국시간 기준 자정 배치 실행일
     * @return 각 단계의 대상·성공·건너뜀·실패 집계
     */
    public DailyBatchExecutionResponse executeDailyBatch(LocalDate batchDate) {
        if (batchDate == null) {
            throw new IllegalArgumentException("일일 배치 기준일은 필수입니다.");
        }

        log.info("챌린지 일일 배치 시작: batchDate={}", batchDate);
        List<String> failures = new ArrayList<>();

        // 주간 성공 횟수를 집계하기 전에 시간 만료 게시물의 승인 상태를 먼저 확정한다.
        int autoApprovedPostCount = autoApproveTimedOutPosts(failures);

        // 마감 임박 알림
        int deadlineNotificationCount =
                notifyUpcomingRoundDeadlines(batchDate.plusDays(1), failures);

        // 아직 ONGOING 상태인 라운드를 대상으로 승인 결과와 성공 횟수를 결산한다.
        WeeklyBatchResult weeklyResult = settleDueWeeklyChallenges(batchDate, failures);
        // 주간 결산을 마친 뒤 전날 종료 라운드를 WAITING_SETTLEMENT로 전환한다.
        GroupBatchResult transitionResult =
                transitionEndedRounds(batchDate.minusDays(1), failures);
        // 정책에 따라 실행일 기준 이틀 전 종료 라운드를 마지막 단계에서 정산한다.
        GroupBatchResult settlementResult =
                settleWaitingRounds(batchDate.minusDays(1), failures);

        log.info("챌린지 일일 배치 종료: batchDate={}", batchDate);

        return DailyBatchExecutionResponse.builder()
                .batchDate(batchDate)
                .autoApprovedPostCount(autoApprovedPostCount)
                .weeklyTargetCount(weeklyResult.targetCount())
                .weeklySettledParticipantCount(weeklyResult.settledParticipantCount())
                .weeklyFailedTargetCount(weeklyResult.failedCount())
                .transitionTargetCount(transitionResult.targetCount())
                .transitionedGroupCount(transitionResult.succeededCount())
                .transitionSkippedCount(transitionResult.skippedCount())
                .transitionFailedGroupCount(transitionResult.failedCount())
                .settlementTargetCount(settlementResult.targetCount())
                .settledGroupCount(settlementResult.succeededCount())
                .settlementSkippedCount(settlementResult.skippedCount())
                .settlementFailedGroupCount(settlementResult.failedCount())
                .failures(failures)
                .build();
    }

    /** 시간이 만료된 PENDING 게시글을 승인하여 이후 결산이 확정 상태를 사용하게 한다. */
    private int autoApproveTimedOutPosts(List<String> failures) {
        try {
            int updatedCount = postMapper.updatePostsToAutoApproved();
            log.info("게시글 자동 승인 완료: updatedCount={}", updatedCount);
            return updatedCount;
        } catch (RuntimeException exception) {
            // 기존 작업과 동일하게 오류를 기록하고 다음 배치 단계는 계속 실행한다.
            log.error("게시글 자동 승인 실패", exception);
            failures.add("stage=post-auto-approval, message=" + exception.getMessage());
            return 0;
        }
    }

    /** 지정일에 주간 집계 주기가 끝난 라운드의 참여자별 성공 횟수를 반영한다. */
    private WeeklyBatchResult settleDueWeeklyChallenges(
            LocalDate settlementDate, List<String> failures) {
        List<WeeklySettlementTarget> targets =
                weeklySettlementService.findDueSettlements(settlementDate);
        int settledParticipantCount = 0;
        int failedTargetCount = 0;

        for (WeeklySettlementTarget target : targets) {
            try {
                // 대상별 서비스 트랜잭션으로 한 라운드의 실패가 다른 라운드를 막지 않게 한다.
                settledParticipantCount += weeklySettlementService.settle(target);
            } catch (RuntimeException exception) {
                failedTargetCount++;
                log.error("주간 챌린지 결산 실패: roundId={}, weekNo={}",
                        target.getRoundId(), target.getWeekNo(), exception);
                failures.add("stage=weekly-settlement, roundId=" + target.getRoundId()
                        + ", weekNo=" + target.getWeekNo()
                        + ", message=" + exception.getMessage());
            }
        }

        log.info("주간 챌린지 결산 완료: date={}, targetCount={}, failedTargetCount={}, "
                        + "successfulParticipantCount={}",
                settlementDate, targets.size(), failedTargetCount, settledParticipantCount);
        return new WeeklyBatchResult(
                targets.size(), settledParticipantCount, failedTargetCount);
    }

    /**
     * 이틀 전 종료된 정산 대기 라운드를 최종 정산한다.
     *
     * <p>서비스 조회 SQL이 전달일에서 하루를 더 빼서 종료일을 비교하므로 자정 배치일의
     * 전날을 {@code settlementDate}로 전달하면 실제 대상은 배치일의 이틀 전이다.</p>
     */
    private GroupBatchResult settleWaitingRounds(
            LocalDate settlementDate, List<String> failures) {
        List<String> groupIds = roundSettlementService.findDueGroupIds(settlementDate);
        int settledCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (String groupId : groupIds) {
            try {
                // 계좌·예치금·원장·이력·라운드 상태는 서비스의 한 트랜잭션에서 반영된다.
                if (roundSettlementService.settleRound(groupId, settlementDate)) {
                    settledCount++;
                } else {
                    skippedCount++;
                }
            } catch (RuntimeException exception) {
                failedCount++;
                // 운영자가 원인을 확인하고 해당 그룹만 다시 처리할 수 있도록 식별자를 남긴다.
                log.error("라운드 자동 정산 실패: groupId={}, settlementDate={}",
                        groupId, settlementDate, exception);
                failures.add("stage=round-settlement, groupId=" + groupId
                        + ", message=" + exception.getMessage());
            }
        }

        log.info("라운드 자동 정산 완료: settlementDate={}, targetCount={}, "
                        + "settledCount={}, failedCount={}",
                settlementDate, groupIds.size(), settledCount, failedCount);
        return new GroupBatchResult(
                groupIds.size(), settledCount, skippedCount, failedCount);
    }

    /** 전날 종료된 진행 중 라운드를 정산 대기로 바꾸고 다음 라운드를 생성한다. */
    private GroupBatchResult transitionEndedRounds(
            LocalDate transitionDate, List<String> failures) {
        // 진행 중 그룹이면서 종료일이 전날인 진행 중 라운드만 대상으로 조회한다.
        List<String> groupIds = roundTransitionService.findDueGroupIds(transitionDate);
        // 실제 전환 완료 그룹 수와 예외 발생 그룹 수를 실행 결과 로그에 집계한다.
        int transitionedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (String groupId : groupIds) {
            try {
                // 그룹별 트랜잭션으로 처리하여 한 그룹의 실패가 다른 그룹에 영향을 주지 않게 한다.
                if (roundTransitionService.transitionToNextRound(groupId, transitionDate)) {
                    // 중복 실행 등으로 이미 처리된 그룹은 false이므로 성공 건수에 포함하지 않는다.
                    transitionedCount++;
                } else {
                    skippedCount++;
                }
            } catch (RuntimeException exception) {
                failedCount++;
                // 실패 그룹은 다음 실행 또는 수동 처리를 위해 식별자와 기준일을 로그에 남긴다.
                log.error("라운드 자동 전환 실패: groupId={}, transitionDate={}",
                        groupId, transitionDate, exception);
                failures.add("stage=round-transition, groupId=" + groupId
                        + ", message=" + exception.getMessage());
            }
        }

        // 대상·성공·실패 건수를 남겨 누락된 그룹이 있는지 운영 로그에서 확인할 수 있게 한다.
        log.info("라운드 자동 전환 완료: transitionDate={}, targetCount={}, "
                        + "transitionedCount={}, failedCount={}",
                transitionDate, groupIds.size(), transitionedCount, failedCount);
        return new GroupBatchResult(
                groupIds.size(), transitionedCount, skippedCount, failedCount);
    }

    /** 주간 결산 단계의 API 응답용 집계값. */
    private record WeeklyBatchResult(
            int targetCount, int settledParticipantCount, int failedCount) {
    }

    /** 그룹 단위 라운드 전환 또는 최종 정산 단계의 API 응답용 집계값. */
    private record GroupBatchResult(
            int targetCount, int succeededCount, int skippedCount, int failedCount) {
    }

    /**
     * 다음 날 종료되는 라운드의 참여자에게 마감 하루 전 알림을 전송한다.
     */
    private int notifyUpcomingRoundDeadlines(
            LocalDate deadlineDate,
            List<String> failures) {

        List<RoundVO> rounds = roundMapper.findRoundsEndingOn(deadlineDate);

        int notificationCount = 0;

        for (RoundVO round : rounds) {
            try {
                List<String> userIds =
                        roundMapper.findRoundNotificationUsers(round.getRoundId());

                for (String userId : userIds) {
                    notificationService.createNotification(
                            userId,
                            NotificationType.REMINDER,
                            round.getRoundNo() + "라운드 마감이 하루 남았습니다."
                    );

                    notificationCount++;
                }

            } catch (RuntimeException exception) {
                log.error(
                        "라운드 마감 임박 알림 실패: roundId={}, roundNo={}",
                        round.getRoundId(),
                        round.getRoundNo(),
                        exception
                );

                failures.add(
                        "stage=round-deadline-notification"
                                + ", roundId=" + round.getRoundId()
                                + ", message=" + exception.getMessage()
                );
            }
        }

        log.info(
                "라운드 마감 임박 알림 완료: deadlineDate={}, notificationCount={}",
                deadlineDate,
                notificationCount
        );

        return notificationCount;
    }
}
