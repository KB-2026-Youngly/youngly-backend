package com.kb.youngly.scheduler;

import com.kb.youngly.dto.round.WeeklySettlementTarget;
import com.kb.youngly.service.WeeklySettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * 주간 챌린지 자동 결산 스케줄러.
 *
 * <p>매일 하루가 시작되는 00:00:00에 실행되어 라운드 시작일을 기준으로
 * 전전날까지 7일간의 주간 집계가 끝난 라운드를 조회하고 사용자별 달성 결과를 반영한다.</p>
 *
 * <p>승인 게시물 집계와 {@code round_history.success_count},
 * {@code group_users.streak_count} 갱신은 서비스 계층에서 처리한다.</p>
 */
@Component
@RequiredArgsConstructor
@Log4j2
public class WeeklySettlementScheduler {

    /** 서버 운영체제의 기본 시간대와 관계없이 한국 날짜를 사용하기 위한 서비스 시간대. */
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final WeeklySettlementService weeklySettlementService;

    /**
     * 매일 한국시간 00시 00분 00초에 주간 결산을 실행한다.
     * 일요일에 시작한 라운드의 첫 결산은 다음 주 월요일 00:00에 실행된다.
     *
     * <p>스케줄러에는 로그인 사용자가 없으므로 개발용 사용자 ID인
     * {@code user01}을 사용하지 않고 조회된 라운드의 전체 참여자를 처리한다.</p>
     *
     * <p>사용자별 주간 결산 결과를 먼저 저장하므로 같은 라운드와 주차를 중복 실행해도
     * 성공 횟수와 streak는 한 번만 증가한다.</p>
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void settleDueWeeklyChallenges() {
        // LocalDate.now()가 서버 기본 시간대를 따르지 않도록 한국 시간대를 명시한다.
        LocalDate settlementDate = LocalDate.now(SERVICE_ZONE);

        // 시작일로부터 8, 15, 22, 29...일이 지난 진행 중 라운드만 조회한다.
        // 일요일부터 토요일까지 집계한 결과를 하루 뒤인 월요일 00:00에 결산한다.
        List<WeeklySettlementTarget> targets =
                weeklySettlementService.findDueSettlements(settlementDate);

        // 작업 종료 로그에 기록할 성공 참여자 수와 실패한 결산 대상 수를 누적한다.
        int settledParticipantCount = 0;
        int failedTargetCount = 0;
        for (WeeklySettlementTarget target : targets) {
            try {
                // 대상마다 별도 트랜잭션으로 처리하여 한 그룹의 실패가 다른 그룹을 막지 않게 한다.
                settledParticipantCount += weeklySettlementService.settle(target);
            } catch (RuntimeException exception) {
                failedTargetCount++;

                // 실패한 라운드와 주차를 기록하여 원인 확인과 수동 재처리에 활용한다.
                log.error("주간 챌린지 결산 실패: roundId={}, weekNo={}",
                        target.getRoundId(), target.getWeekNo(), exception);
            }
        }

        // 전체 대상 수, 실패 대상 수, 목표를 달성한 참여자 수를 실행 결과로 남긴다.
        log.info("주간 챌린지 결산 완료: date={}, targetCount={}, failedTargetCount={}, "
                        + "successfulParticipantCount={}",
                settlementDate, targets.size(), failedTargetCount, settledParticipantCount);
    }
}
