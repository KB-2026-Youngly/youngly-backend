package com.kb.youngly.service;

import com.kb.youngly.dto.recommendation.PensionForecastFacts;
import com.kb.youngly.dto.recommendation.PensionForecastParticipant;
import com.kb.youngly.dto.recommendation.PensionForecastSource;
import com.kb.youngly.mapper.PensionForecastMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PensionForecastService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int TOTAL_ROUND_WEEKS = 4;
    private static final int MONEY_SCALE = 2;

    private final PensionForecastMapper pensionForecastMapper;

    public PensionForecastService(PensionForecastMapper pensionForecastMapper) {
        this.pensionForecastMapper = pensionForecastMapper;
    }

    public PensionForecastFacts calculateForecast(String userId) {
        List<PensionForecastSource> sources =
                pensionForecastMapper.findOngoingForecastSources(userId);

        BigDecimal settled = zeroIfNull(
                pensionForecastMapper.sumConfirmedPensionSettlementThisMonth(userId)
        );

        BigDecimal currentPensionBalance = zeroIfNull(
                pensionForecastMapper.findPensionBalance(userId)
        );

        LocalDate today = LocalDate.now();
        LocalDate periodStartDate = today.withDayOfMonth(1);
        LocalDate periodEndDate = today.withDayOfMonth(today.lengthOfMonth());

        BigDecimal additionalConservative = BigDecimal.ZERO;
        BigDecimal additionalCurrentRank = BigDecimal.ZERO;
        BigDecimal additionalBestCase = BigDecimal.ZERO;

        List<PensionForecastFacts.GroupContribution> contributions = new ArrayList<>();

        for (PensionForecastSource source : sources) {

            // Mapper가 이번 달 end_date + ONGOING/WAITING_SETTLEMENT만 조회한다.
            // 방어적으로 기간 밖 라운드는 한 번 더 건너뛴다.
            if (source.getRoundEndDate() == null
                    || source.getRoundEndDate().isBefore(periodStartDate)
                    || source.getRoundEndDate().isAfter(periodEndDate)) {
                continue;
            }

            Map<Integer, BigDecimal> ratiosByRank =
                    parseRatioRule(source.getFutureDepositRatioRule());

            if (ratiosByRank.isEmpty()) {
                continue;
            }

            List<PensionForecastParticipant> participants = pensionForecastMapper
                    .findRoundForecastParticipants(source.getRoundId());

            boolean hasParticipantData = participants != null && !participants.isEmpty();
            int remainingWeeks = remainingWeeks(source.getCompletedWeekCount());

            Integer currentRank = source.getRankNo();
            Integer conservativeRank = currentRank;
            Integer optimisticRank = null;

            if (hasParticipantData) {
                currentRank = calculateRank(participants, userId, 0, 0);
                conservativeRank = calculateRank(participants, userId, 0, remainingWeeks);
                optimisticRank = calculateRank(participants, userId, remainingWeeks, 0);
            }

            BigDecimal contributionConservative = calculateSettlementAmount(
                    source.getBaseDepositAmount(),
                    ratiosByRank.get(conservativeRank)
            );

            BigDecimal contributionCurrent = calculateSettlementAmount(
                    source.getBaseDepositAmount(),
                    ratiosByRank.get(currentRank)
            );

            BigDecimal contributionOptimistic = hasParticipantData
                    ? calculateSettlementAmount(
                            source.getBaseDepositAmount(),
                            ratiosByRank.get(optimisticRank)
                    )
                    : calculateSettlementAmount(
                            source.getBaseDepositAmount(),
                            ratiosByRank.values().stream()
                                    .max(Comparator.naturalOrder())
                                    .orElse(null)
                    );

            BigDecimal contributionMin = minimum(
                    contributionConservative,
                    contributionCurrent,
                    contributionOptimistic
            );
            BigDecimal contributionMax = maximum(
                    contributionConservative,
                    contributionCurrent,
                    contributionOptimistic
            );

            additionalConservative = additionalConservative.add(contributionMin);
            additionalCurrentRank = additionalCurrentRank.add(contributionCurrent);
            additionalBestCase = additionalBestCase.add(contributionMax);

            double weeklySuccessRate = weeklySuccessRate(
                    source.getSuccessCount(),
                    source.getCompletedWeekCount()
            );

            contributions.add(new PensionForecastFacts.GroupContribution(
                    source.getGroupName(),
                    source.getChallengeType(),
                    source.getSuccessCount(),
                    currentRank,
                    weeklySuccessRate,
                    contributionMin,
                    contributionMax,
                    source.getRoundEndDate()
            ));
        }

        LocalDate nextDepositDate = contributions.stream()
                .map(PensionForecastFacts.GroupContribution::roundEndDate)
                .filter(date -> date != null)
                .min(Comparator.naturalOrder())
                .orElse(null);

        BigDecimal expectedTotal = money(settled.add(additionalCurrentRank));
        BigDecimal expectedMaxTotal = money(settled.add(additionalBestCase));

        return new PensionForecastFacts(
                userId,
                periodStartDate,
                periodEndDate,
                money(settled),
                money(additionalConservative),
                money(additionalCurrentRank),
                money(additionalBestCase),
                expectedTotal,
                expectedMaxTotal,
                currentPensionBalance,
                !contributions.isEmpty(),
                nextDepositDate,
                List.copyOf(contributions)
        );
    }

    Map<Integer, BigDecimal> parseRatioRule(String rule) {
        Map<Integer, BigDecimal> ratios = new HashMap<>();

        if (rule == null || rule.trim().isEmpty()) {
            return ratios;
        }

        for (String entry : rule.split("/", -1)) {
            String[] parts = entry.trim().split(":", -1);

            if (parts.length != 2) {
                continue;
            }

            try {
                int rank = Integer.parseInt(parts[0].trim());
                BigDecimal ratio = new BigDecimal(parts[1].trim());

                if (rank <= 0
                        || ratio.signum() < 0
                        || ratio.compareTo(ONE_HUNDRED) > 0) {
                    continue;
                }

                ratios.putIfAbsent(rank, ratio);

            } catch (NumberFormatException ignored) {
                // 잘못된 비율 규칙은 건너뛴다.
            }
        }

        return ratios;
    }

    private BigDecimal calculateSettlementAmount(
            BigDecimal baseAmount,
            BigDecimal ratio
    ) {
        if (ratio == null || baseAmount == null) {
            return BigDecimal.ZERO.setScale(
                    MONEY_SCALE,
                    RoundingMode.HALF_UP
            );
        }

        return baseAmount.multiply(ratio)
                .divide(
                        ONE_HUNDRED,
                        MONEY_SCALE,
                        RoundingMode.HALF_UP
                );
    }

    private int remainingWeeks(Integer completedWeekCount) {
        int completed = completedWeekCount == null ? 0 : completedWeekCount;
        return Math.max(0, TOTAL_ROUND_WEEKS - Math.min(completed, TOTAL_ROUND_WEEKS));
    }

    private BigDecimal minimum(BigDecimal... amounts) {
        return List.of(amounts).stream()
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal maximum(BigDecimal... amounts) {
        return List.of(amounts).stream()
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
    }

    /**
     * RANK()와 동일하게 자신보다 성공 주차 수가 많은 참여자 수에 1을 더한다.
     * userAdditionalSuccess와 peerAdditionalSuccess는 남은 주차의 시나리오 가정이다.
     */
    private int calculateRank(
            List<PensionForecastParticipant> participants,
            String userId,
            int userAdditionalSuccess,
            int peerAdditionalSuccess
    ) {
        int userSuccessCount = participants.stream()
                .filter(participant -> userId.equals(participant.getUserId()))
                .map(PensionForecastParticipant::getSuccessCount)
                .filter(successCount -> successCount != null)
                .findFirst()
                .orElse(0);

        int assumedUserSuccessCount = Math.min(
                TOTAL_ROUND_WEEKS,
                Math.max(0, userSuccessCount) + userAdditionalSuccess
        );

        long higherSuccessCount = participants.stream()
                .filter(participant -> !userId.equals(participant.getUserId()))
                .map(PensionForecastParticipant::getSuccessCount)
                .map(successCount -> successCount == null ? 0 : successCount)
                .mapToInt(successCount -> Math.min(
                        TOTAL_ROUND_WEEKS,
                        Math.max(0, successCount) + peerAdditionalSuccess
                ))
                .filter(successCount -> successCount > assumedUserSuccessCount)
                .count();

        return Math.toIntExact(higherSuccessCount + 1);
    }

    /**
     * 주간 인증 성공률 = 성공 주차 수 / 결산 완료된 주차 수.
     * min_count(주간 최소 게시물 수)와는 단위가 다르므로 사용하지 않는다.
     * successCount가 completedWeekCount를 넘는 불일치 fixture/데이터는 1.0으로 클램프한다.
     */
    private double weeklySuccessRate(
            Integer successCount,
            Integer completedWeekCount
    ) {
        if (successCount == null
                || completedWeekCount == null
                || completedWeekCount <= 0
                || successCount <= 0) {
            return 0.0;
        }

        double rate = BigDecimal.valueOf(successCount)
                .divide(
                        BigDecimal.valueOf(completedWeekCount),
                        4,
                        RoundingMode.HALF_UP
                )
                .doubleValue();

        if (rate > 1.0) {
            return 1.0;
        }
        return rate;
    }

    private BigDecimal money(BigDecimal value) {
        return zeroIfNull(value).setScale(
                MONEY_SCALE,
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : value;
    }
}
