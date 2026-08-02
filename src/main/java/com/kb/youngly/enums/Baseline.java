package com.kb.youngly.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 투자성향(Baseline) 5단계 분류.
 * DB survey_results.baseline 및 API 응답에는 {@link #label}(한글)을 사용한다.
 */
@Getter
@RequiredArgsConstructor
public enum Baseline {
    STABLE("신중한 저축형", 6, 10),
    CONSERVATIVE("균형 잡힌 성장형", 11, 15),
    NEUTRAL("도전하는 균형형", 16, 20),
    AGGRESSIVE("적극적인 성장형", 21, 25),
    VERY_AGGRESSIVE("과감한 도전형", 26, 30);

    private final String label;
    private final int minScore;
    private final int maxScore;

    public static Baseline fromTotalScore(int totalScore) {
        for (Baseline baseline : values()) {
            if (totalScore >= baseline.minScore && totalScore <= baseline.maxScore) {
                return baseline;
            }
        }
        throw new IllegalArgumentException("총점 범위가 올바르지 않습니다: " + totalScore);
    }
}
