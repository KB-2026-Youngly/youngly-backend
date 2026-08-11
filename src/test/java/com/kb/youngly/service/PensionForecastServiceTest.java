package com.kb.youngly.service;

import com.kb.youngly.dto.recommendation.PensionForecastSource;
import com.kb.youngly.mapper.PensionForecastMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * future_deposit_ratio_rule 해석은 실제 정산 로직(RoundSettlementServiceImpl,
 * PR #134 / YL-81 라운드 자동 정산 로직)과 반드시 동일해야 한다.
 *
 *   "1:40/2:60/3:80" -> {1:40, 2:60, 3:80} (퍼센트 정수 그대로)
 *   settlementAmount = baseDepositAmount * ratiosByRank.get(rankNo) / 100
 *   weeklySuccessRate = successCount / completedWeekCount (min_count 사용 금지, 0~1 클램프)
 *   completedWeekCount = weekly_settlements 중 created_at <= NOW() 인 DISTINCT week_no
 *   임시 순위는 round_history.success_count 기준 RANK()이며 streak_count를 쓰지 않는다.
 *   규칙에 없는 순위는 정확히 0원이다 (다른 순위 비율을 대신 쓰지 않는다).
 */
class PensionForecastServiceTest {

    @Test
    void calculatesForecastFromOngoingGroupsAndConfirmedSettlement() {
        PensionForecastMapper mapper = mock(PensionForecastMapper.class);
        when(mapper.findOngoingForecastSources("user01")).thenReturn(List.of(
                new PensionForecastSource(
                        "group-exercise-01",
                        "아침 운동 챌린지",
                        "EXERCISE",
                        new BigDecimal("200000.00"),
                        "1:40/2:60/3:80",
                        3,
                        4,
                        1,
                        4,
                        LocalDate.of(2026, 8, 22)
                ),
                new PensionForecastSource(
                        "group-reading-01",
                        "독서 습관 챌린지",
                        "READING",
                        new BigDecimal("100000.00"),
                        "1:40/2:60/3:80",
                        4,
                        3,
                        2,
                        4,
                        LocalDate.of(2026, 8, 18)
                )
        ));
        when(mapper.sumConfirmedPensionSettlementThisMonth("user01")).thenReturn(new BigDecimal("30000.00"));
        when(mapper.findPensionBalance("user01")).thenReturn(new BigDecimal("5000000.00"));

        PensionForecastService service = new PensionForecastService(mapper);
        var facts = service.calculateForecast("user01");

        assertEquals(new BigDecimal("30000.00"), facts.settledAmountThisMonth());
        assertEquals(new BigDecimal("140000.00"), facts.expectedAdditionalAmountCurrentRank());
        assertEquals(new BigDecimal("240000.00"), facts.expectedAdditionalAmountBestCase());
        assertEquals(new BigDecimal("170000.00"), facts.expectedTotalAmountThisMonth());
        assertEquals(new BigDecimal("270000.00"), facts.expectedMaxTotalAmountThisMonth());
        assertEquals(1.0, facts.groupContributions().get(0).weeklySuccessRate());
        assertEquals(0.75, facts.groupContributions().get(1).weeklySuccessRate());
        assertEquals(4, facts.groupContributions().get(0).successCount());
        assertEquals(1, facts.groupContributions().get(0).provisionalRank());
        assertEquals(3, facts.groupContributions().get(1).successCount());
        assertEquals(2, facts.groupContributions().get(1).provisionalRank());
        assertEquals(new BigDecimal("60000.00"), facts.groupContributions().get(1).contributionMin());
        assertEquals(new BigDecimal("80000.00"), facts.groupContributions().get(1).contributionMax());
    }

    @Test
    void calculatesAiuser02DemoForecast_exerciseRank1_readingRank2() {
        // 2026-08-11 기준 data.sql aiuser02 mock:
        // 운동: 완료 주차 1(week1만), successCount=1 → RANK 1위 → 200000×40%=80000
        // 독서: 완료 주차 4, successCount=3 → RANK 2위 → 100000×70%=70000
        // 추가 예상 150000 / best 250000 (순위·비율 규칙은 동일하게 유지)
        PensionForecastMapper mapper = mock(PensionForecastMapper.class);
        when(mapper.findOngoingForecastSources("aiuser02")).thenReturn(List.of(
                new PensionForecastSource(
                        "group-aiuser02-reading",
                        "독서 습관 챌린지(AI테스트)",
                        "READING",
                        new BigDecimal("100000.00"),
                        "1:50/2:70/3:90",
                        4,
                        3,
                        2,
                        4,
                        LocalDate.of(2026, 8, 10)
                ),
                new PensionForecastSource(
                        "group-aiuser02-exercise",
                        "아침 운동 챌린지(AI테스트)",
                        "EXERCISE",
                        new BigDecimal("200000.00"),
                        "1:40/2:60/3:80",
                        3,
                        1,
                        1,
                        1,
                        LocalDate.of(2026, 8, 25)
                )
        ));
        when(mapper.sumConfirmedPensionSettlementThisMonth("aiuser02"))
                .thenReturn(new BigDecimal("30000.00"));
        when(mapper.findPensionBalance("aiuser02"))
                .thenReturn(new BigDecimal("5030000.00"));

        PensionForecastService service = new PensionForecastService(mapper);
        var facts = service.calculateForecast("aiuser02");

        assertEquals(new BigDecimal("30000.00"), facts.settledAmountThisMonth());
        assertEquals(new BigDecimal("150000.00"), facts.expectedAdditionalAmountCurrentRank());
        assertEquals(new BigDecimal("250000.00"), facts.expectedAdditionalAmountBestCase());
        assertEquals(new BigDecimal("180000.00"), facts.expectedTotalAmountThisMonth());
        assertEquals(new BigDecimal("280000.00"), facts.expectedMaxTotalAmountThisMonth());

        assertEquals(2, facts.groupContributions().size());
        var reading = facts.groupContributions().get(0);
        var exercise = facts.groupContributions().get(1);

        assertEquals(3, reading.successCount());
        assertEquals(2, reading.provisionalRank());
        assertEquals(0.75, reading.weeklySuccessRate());
        assertEquals(new BigDecimal("70000.00"), reading.contributionMin());
        assertEquals(new BigDecimal("90000.00"), reading.contributionMax());

        assertEquals(1, exercise.successCount());
        assertEquals(1, exercise.provisionalRank());
        assertEquals(1.0, exercise.weeklySuccessRate());
        assertEquals(new BigDecimal("80000.00"), exercise.contributionMin());
        assertEquals(new BigDecimal("160000.00"), exercise.contributionMax());
    }

    @Test
    void weeklySuccessRateIsClampedToOneWhenSuccessExceedsCompletedWeeks() {
        PensionForecastMapper mapper = mock(PensionForecastMapper.class);
        when(mapper.findOngoingForecastSources("user01")).thenReturn(List.of(
                new PensionForecastSource(
                        "group-exercise-01",
                        "아침 운동 챌린지",
                        "EXERCISE",
                        new BigDecimal("200000.00"),
                        "1:40/2:60/3:80",
                        3,
                        3,
                        1,
                        1,
                        LocalDate.of(2026, 8, 22)
                )
        ));
        when(mapper.sumConfirmedPensionSettlementThisMonth("user01")).thenReturn(BigDecimal.ZERO.setScale(2));
        when(mapper.findPensionBalance("user01")).thenReturn(new BigDecimal("1000.00"));

        var facts = new PensionForecastService(mapper).calculateForecast("user01");

        assertEquals(1.0, facts.groupContributions().get(0).weeklySuccessRate());
    }

    @Test
    void weeklySuccessRateIsZeroWhenNoCompletedWeeks() {
        PensionForecastMapper mapper = mock(PensionForecastMapper.class);
        when(mapper.findOngoingForecastSources("user01")).thenReturn(List.of(
                new PensionForecastSource(
                        "group-exercise-01",
                        "아침 운동 챌린지",
                        "EXERCISE",
                        new BigDecimal("200000.00"),
                        "1:40/2:60/3:80",
                        3,
                        2,
                        1,
                        0,
                        LocalDate.of(2026, 8, 22)
                )
        ));
        when(mapper.sumConfirmedPensionSettlementThisMonth("user01")).thenReturn(BigDecimal.ZERO.setScale(2));
        when(mapper.findPensionBalance("user01")).thenReturn(new BigDecimal("1000.00"));

        var facts = new PensionForecastService(mapper).calculateForecast("user01");

        assertEquals(0.0, facts.groupContributions().get(0).weeklySuccessRate());
    }

    @Test
    void returnsConfirmedOnlyWhenNoOngoingGroupsExist() {
        PensionForecastMapper mapper = mock(PensionForecastMapper.class);
        when(mapper.findOngoingForecastSources("user01")).thenReturn(List.of());
        when(mapper.sumConfirmedPensionSettlementThisMonth("user01")).thenReturn(new BigDecimal("30000.00"));
        when(mapper.findPensionBalance("user01")).thenReturn(new BigDecimal("5000000.00"));

        PensionForecastService service = new PensionForecastService(mapper);
        var facts = service.calculateForecast("user01");

        assertEquals(new BigDecimal("30000.00"), facts.settledAmountThisMonth());
        assertEquals(new BigDecimal("0.00"), facts.expectedAdditionalAmountCurrentRank());
        assertEquals(new BigDecimal("0.00"), facts.expectedAdditionalAmountBestCase());
        assertEquals(new BigDecimal("30000.00"), facts.expectedTotalAmountThisMonth());
        assertEquals(new BigDecimal("30000.00"), facts.expectedMaxTotalAmountThisMonth());
        assertFalse(facts.hasOngoingRoundThisMonth());
        assertNull(facts.nextDepositDate());
        assertTrue(facts.groupContributions().isEmpty());
    }

    @Test
    void skipsRoundsEndingAfterThisMonth() {
        PensionForecastMapper mapper = mock(PensionForecastMapper.class);
        LocalDate today = LocalDate.now();
        LocalDate thisMonthEnd = today.withDayOfMonth(today.lengthOfMonth());
        LocalDate nextMonth = today.plusMonths(1);
        LocalDate nextMonthEnd = nextMonth.withDayOfMonth(nextMonth.lengthOfMonth());

        when(mapper.findOngoingForecastSources("user01")).thenReturn(List.of(
                new PensionForecastSource(
                        "group-exercise-01",
                        "아침 운동 챌린지",
                        "EXERCISE",
                        new BigDecimal("200000.00"),
                        "1:40/2:60/3:80",
                        3,
                        4,
                        1,
                        4,
                        thisMonthEnd
                ),
                new PensionForecastSource(
                        "group-reading-01",
                        "독서 습관 챌린지",
                        "READING",
                        new BigDecimal("100000.00"),
                        "1:40/2:60/3:80",
                        4,
                        3,
                        2,
                        4,
                        nextMonthEnd
                )
        ));
        when(mapper.sumConfirmedPensionSettlementThisMonth("user01")).thenReturn(new BigDecimal("30000.00"));
        when(mapper.findPensionBalance("user01")).thenReturn(new BigDecimal("5000000.00"));

        PensionForecastService service = new PensionForecastService(mapper);
        var facts = service.calculateForecast("user01");

        assertTrue(facts.hasOngoingRoundThisMonth());
        assertEquals(1, facts.groupContributions().size());
        assertEquals(thisMonthEnd, facts.nextDepositDate());
    }

    @Test
    void rankNotDefinedInRuleYieldsZeroContribution() {
        PensionForecastMapper mapper = mock(PensionForecastMapper.class);
        when(mapper.findOngoingForecastSources("user01")).thenReturn(List.of(
                new PensionForecastSource(
                        "group-exercise-01",
                        "아침 운동 챌린지",
                        "EXERCISE",
                        new BigDecimal("200000.00"),
                        "1:40/2:60/3:80",
                        3,
                        4,
                        4,
                        4,
                        LocalDate.of(2026, 8, 22)
                )
        ));
        when(mapper.sumConfirmedPensionSettlementThisMonth("user01")).thenReturn(BigDecimal.ZERO.setScale(2));
        when(mapper.findPensionBalance("user01")).thenReturn(new BigDecimal("5000000.00"));

        PensionForecastService service = new PensionForecastService(mapper);
        var facts = service.calculateForecast("user01");

        assertEquals(new BigDecimal("0.00"), facts.groupContributions().get(0).contributionMin());
        assertEquals(new BigDecimal("160000.00"), facts.groupContributions().get(0).contributionMax());
    }

    @Test
    void parsesRankRatioRule() {
        PensionForecastService service = new PensionForecastService(mock(PensionForecastMapper.class));

        Map<Integer, BigDecimal> ratios = service.parseRatioRule("1:40/2:60/3:80");

        assertEquals(0, new BigDecimal("40").compareTo(ratios.get(1)));
        assertEquals(0, new BigDecimal("60").compareTo(ratios.get(2)));
        assertEquals(0, new BigDecimal("80").compareTo(ratios.get(3)));
        assertNull(ratios.get(4));
    }

    @Test
    void malformedRuleReturnsEmptyMap() {
        PensionForecastService service = new PensionForecastService(mock(PensionForecastMapper.class));

        Map<Integer, BigDecimal> ratios = service.parseRatioRule("lowest 40%, highest 80%");

        assertTrue(ratios.isEmpty());
    }
}
