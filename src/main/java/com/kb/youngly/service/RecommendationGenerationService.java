package com.kb.youngly.service;

import com.kb.youngly.event.SurveyResultSavedEvent;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@Log4j2
public class RecommendationGenerationService {

    private final RecommendationGenerationAsyncService asyncService;

    public RecommendationGenerationService(RecommendationGenerationAsyncService asyncService) {
        this.asyncService = asyncService;
    }

    public void request(String userId, Long surveyResultId) {
        log.info("[RECOMMENDATION_GENERATION_REQUESTED] userId={}, surveyResultId={}", userId, surveyResultId);
        asyncService.generate(userId, surveyResultId);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAfterCommit(SurveyResultSavedEvent event) {
        request(event.userId(), event.surveyResultId());
    }

    @EventListener
    public void handleWithoutTransaction(SurveyResultSavedEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            request(event.userId(), event.surveyResultId());
        }
    }
}
