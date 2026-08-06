package com.kb.youngly.scheduler;

import com.kb.youngly.service.RoundTransitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * 종료된 라운드를 정산 대기로 전환하고 다음 라운드를 자동 생성하는 스케줄러.
 *
 * <p>한국시간 기준 매일 23시 59분에 실행되며 다음 조건을 모두 만족하는
 * 라운드를 처리한다.</p>
 *
 * <ul>
 *     <li>그룹 상태가 {@code ONGOING}일 것</li>
 *     <li>라운드 상태가 {@code ONGOING}일 것</li>
 *     <li>라운드 종료일이 실행 당일과 같을 것</li>
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

    /**
     * 매일 한국시간 23시 59분에 당일 종료 라운드를 전환한다.
     *
     * <p>각 그룹을 별도로 처리하여 특정 그룹에서 데이터 오류가 발생하더라도
     * 나머지 그룹의 자동 전환은 계속 진행한다. 처리 결과는 운영 확인을 위해
     * 성공 및 실패 건수와 함께 로그로 남긴다.</p>
     */
    @Scheduled(cron = "0 59 23 * * *", zone = "Asia/Seoul")
    public void transitionEndedRounds() {
        // 조회 조건과 새 라운드 날짜 계산에 사용할 한국 기준 실행일을 확정한다.
        LocalDate transitionDate = LocalDate.now(SERVICE_ZONE);

        // 진행 중 그룹이면서 종료일이 오늘인 진행 중 라운드만 대상으로 조회한다.
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
