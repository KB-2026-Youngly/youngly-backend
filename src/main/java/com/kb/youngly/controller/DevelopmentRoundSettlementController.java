package com.kb.youngly.controller;

import com.kb.youngly.dto.round.RoundSettlementExecutionResponse;
import com.kb.youngly.service.RoundSettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Postman에서 매일 23:59 자동 정산과 동일한 로직을 실행하는 개발 전용 API.
 *
 * <p>실제 계좌 잔액, 예치금, 거래 원장 및 라운드 상태를 변경하므로 테스트 DB에서
 * 사용해야 한다. 운영 배포 전에는 이 Controller와 {@code SecurityConfig}의
 * {@code /api/dev/**} 허용 설정을 제거해야 한다.</p>
 */
@RestController
@RequestMapping("/api/dev/round-settlements")
@RequiredArgsConstructor
public class DevelopmentRoundSettlementController {

    /** 스케줄러와 동일한 대상 조회 및 그룹별 정산 트랜잭션을 수행한다. */
    private final RoundSettlementService roundSettlementService;

    /**
     * 전달받은 날짜를 한국시간 스케줄 실행일로 간주하여 라운드를 정산한다.
     *
     * <p>예: 종료일이 2026-08-05인 라운드를 테스트하려면
     * {@code POST /api/dev/round-settlements?date=2026-08-06}으로 호출한다.</p>
     *
     * <p>대상 그룹마다 별도의 서비스 트랜잭션을 사용하므로 한 그룹이 실패해도
     * 나머지 그룹은 계속 처리된다. 이미 다른 요청이 정산한 그룹은 false가 반환되어
     * {@code skippedCount}에 포함된다.</p>
     *
     * @param date {@code rounds.end_date + 1일}과 비교할 정산 기준일
     * @return 대상, 성공, 건너뜀, 실패 건수와 그룹별 실패 사유
     */
    @PostMapping
    public ResponseEntity<RoundSettlementExecutionResponse> settle(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        // 스케줄러와 같은 조건으로 ONGOING 그룹의 전날 종료된 WAITING_SETTLEMENT를 조회한다.
        List<String> groupIds = roundSettlementService.findDueGroupIds(date);
        List<String> failures = new ArrayList<>();
        int settledCount = 0;
        int skippedCount = 0;

        for (String groupId : groupIds) {
            try {
                // true는 정산 완료, false는 잠금 후 재조회에서 대상이 사라진 경우다.
                if (roundSettlementService.settleRound(groupId, date)) {
                    settledCount++;
                } else {
                    skippedCount++;
                }
            } catch (RuntimeException exception) {
                // 그룹별 실패를 응답에 남기되 다른 그룹의 정산은 계속 진행한다.
                failures.add("groupId=" + groupId + ", message=" + exception.getMessage());
            }
        }

        RoundSettlementExecutionResponse response =
                RoundSettlementExecutionResponse.builder()
                        .settlementDate(date)
                        .targetCount(groupIds.size())
                        .settledCount(settledCount)
                        .skippedCount(skippedCount)
                        .failedCount(failures.size())
                        .failures(failures)
                        .build();

        return ResponseEntity.ok(response);
    }
}
