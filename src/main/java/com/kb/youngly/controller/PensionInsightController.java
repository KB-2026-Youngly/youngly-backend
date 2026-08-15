package com.kb.youngly.controller;

import com.kb.youngly.dto.recommendation.PensionInsightPageResponse;
import com.kb.youngly.service.PensionInsightPageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pension/insight")
public class PensionInsightController {
    private final PensionInsightPageService pensionInsightPageService;

    public PensionInsightController(PensionInsightPageService pensionInsightPageService) {
        this.pensionInsightPageService = pensionInsightPageService;
    }

    @GetMapping("/{userId}")
    public PensionInsightPageResponse getInsight(@PathVariable String userId) {
        return pensionInsightPageService.getInsightPage(userId);
    }
}
