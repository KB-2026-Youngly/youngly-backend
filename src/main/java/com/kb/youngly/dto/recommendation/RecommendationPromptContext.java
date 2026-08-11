package com.kb.youngly.dto.recommendation;

import java.util.List;

public record RecommendationPromptContext(
        String userId,
        String ageBand,
        String baseline,
        String baselineLabel,
        List<String> investmentInterests,
        List<String> hobbyInterests,
        String recentMarketContext,
        PensionForecastFacts forecastFacts
) {
}
