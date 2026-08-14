package com.kb.youngly.service;

import com.kb.youngly.event.SurveyResultSavedEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RecommendationGenerationServiceTest {

    @Test
    void afterCommitEventRequestsGenerationOnceWithSurveyResultId() {
        RecommendationGenerationAsyncService asyncService = mock(RecommendationGenerationAsyncService.class);
        RecommendationGenerationService service = new RecommendationGenerationService(asyncService);

        service.handleAfterCommit(new SurveyResultSavedEvent("user01", 2008L));

        verify(asyncService).generate("user01", 2008L);
    }
}
