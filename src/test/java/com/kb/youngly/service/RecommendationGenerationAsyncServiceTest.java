package com.kb.youngly.service;

import com.kb.youngly.dto.recommendation.RecommendationResponse;
import com.kb.youngly.enums.GenerationMode;
import com.kb.youngly.enums.GuardrailStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class RecommendationGenerationAsyncServiceTest {

    @Test
    void asyncGenerationUsesProvidedSurveyResultId() {
        RecommendationService recommendationService = mock(RecommendationService.class);
        when(recommendationService.generateRecommendation("user01", 2008L))
                .thenReturn(RecommendationResponse.llmTextOnly(List.of(), null, null, null)
                        .withGenerationMode(GenerationMode.LIVE));

        RecommendationGenerationAsyncService service = new RecommendationGenerationAsyncService(recommendationService);

        service.generate("user01", 2008L);

        verify(recommendationService).generateRecommendation("user01", 2008L);
    }

    @Test
    void asyncGenerationSavesSafeDefaultWhenUnexpectedExceptionOccurs() {
        RecommendationService recommendationService = mock(RecommendationService.class);
        RuntimeException failure = new RuntimeException("boom");
        when(recommendationService.generateRecommendation("user01", 2008L)).thenThrow(failure);

        RecommendationGenerationAsyncService service = new RecommendationGenerationAsyncService(recommendationService);

        service.generate("user01", 2008L);

        verify(recommendationService).saveSafeDefaultOnUnexpectedFailure("user01", 2008L, failure);
    }
}
