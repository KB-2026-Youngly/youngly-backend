package com.kb.youngly.scheduler;

import com.kb.youngly.service.RoundTransitionService;
import com.kb.youngly.service.RoundSettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * 종료된 라운드의 정산과 다음 라운드 전환을 자동 실행하는 스케줄러.
 *
 * <p>라운드 전환은 한국시간 기준 매일 00시에 전날 종료된 라운드를 대상으로
 * 실행하고, 라운드 정산은 매일 23시 59분에 처리한다.</p>
 *
 * <ul>
 *     <li>그룹 상태가 {@code ONGOING}일 것</li>
 *     <li>라운드 상태가 {@code ONGOING}일 것</li>
 *     <li>라운드 종료일이 전환 작업 실행일의 전날과 같을 것</li>
 * </ul>
 *
 * <p>실제 상태 변경과 다음 라운드 생성은 서비스 계층에서 하나의 트랜잭션으로
 * 처리한다. 따라서 다음 라운드 또는 참여 이력 생성에 실패하면 종료 라운드의
 * 상태 변경도 함께 롤백된다.</p>
 */
@Component
@RequiredArgsConstructor
@Log4j2
public class RoundTransitionScheduler {

    /** 서버 기본 시간대와 무관하게 한국 날짜를 사용한다. */
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    /** 전환 대상 조회와 그룹별 라운드 전환 트랜잭션을 수행하는 서비스. */
    private final RoundTransitionService roundTransitionService;

    /** 종료일 다음 날이 된 라운드의 계좌 이체와 정산 완료 처리를 수행하는 서비스. */
    private final RoundSettlementService roundSettlementService;

    /**
     * 매일 한국시간 00시에 이틀전 종료된 정산 대기 라운드를 정산한다.
     *
     * <p>공동 순위를 포함한 참여자별 적립금 이체는 그룹별 독립 트랜잭션으로
     * 실행한다. 한 그룹의 계좌 잔액이나 규칙에 문제가 있어도 다른 그룹은 계속
     * 처리하며, 실패한 그룹은 WAITING_SETTLEMENT 상태로 남아 재처리할 수 있다.</p>
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void settleWaitingRounds() {
        LocalDate settlementDate = LocalDate.now(SERVICE_ZONE).minusDays(1);
        List<String> groupIds = roundSettlementService.findDueGroupIds(settlementDate);
        int settledCount = 0;
        int failedCount = 0;

        for (String groupId : groupIds) {
            try {
                // 계좌·예치금·원장·이력·라운드 상태는 서비스의 한 트랜잭션에서 반영된다.
                if (roundSettlementService.settleRound(groupId, settlementDate)) {
                    settledCount++;
                }
            } catch (RuntimeException exception) {
                failedCount++;
                // 운영자가 원인을 확인하고 해당 그룹만 다시 처리할 수 있도록 식별자를 남긴다.
                log.error("라운드 자동 정산 실패: groupId={}, settlementDate={}",
                        groupId, settlementDate, exception);
            }
        }

        log.info("라운드 자동 정산 완료: settlementDate={}, targetCount={}, "
                        + "settledCount={}, failedCount={}",
                settlementDate, groupIds.size(), settledCount, failedCount);
    }

    /**
     * 매일 한국시간 00시에 전날 종료된 라운드를 전환한다.
     *
     * <p>각 그룹을 별도로 처리하여 특정 그룹에서 데이터 오류가 발생하더라도
     * 나머지 그룹의 자동 전환은 계속 진행한다. 처리 결과는 운영 확인을 위해
     * 성공 및 실패 건수와 함께 로그로 남긴다.</p>
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void transitionEndedRounds() {
        // 00시 실행 시점에는 날짜가 바뀌었으므로 라운드 종료일과 비교할 전날을 사용한다.
        LocalDate transitionDate = LocalDate.now(SERVICE_ZONE).minusDays(1);

        // 진행 중 그룹이면서 종료일이 전날인 진행 중 라운드만 대상으로 조회한다.
        List<String> groupIds = roundTransitionService.findDueGroupIds(transitionDate);
        // 실제 전환 완료 그룹 수와 예외 발생 그룹 수를 실행 결과 로그에 집계한다.
        int transitionedCount = 0;
        int failedCount = 0;

        for (String groupId : groupIds) {
            try {
                // 그룹별 트랜잭션으로 처리하여 한 그룹의 실패가 다른 그룹에 영향을 주지 않게 한다.
                if (roundTransitionService.transitionToNextRound(groupId, transitionDate)) {
                    // 중복 실행 등으로 이미 처리된 그룹은 false이므로 성공 건수에 포함하지 않는다.
                    transitionedCount++;
                }
            } catch (RuntimeException exception) {
                failedCount++;
                // 실패 그룹은 다음 실행 또는 수동 처리를 위해 식별자와 기준일을 로그에 남긴다.
                log.error("라운드 자동 전환 실패: groupId={}, transitionDate={}",
                        groupId, transitionDate, exception);
            }
        }

        // 대상·성공·실패 건수를 남겨 누락된 그룹이 있는지 운영 로그에서 확인할 수 있게 한다.
        log.info("라운드 자동 전환 완료: transitionDate={}, targetCount={}, "
                        + "transitionedCount={}, failedCount={}",
                transitionDate, groupIds.size(), transitionedCount, failedCount);
    }
}
