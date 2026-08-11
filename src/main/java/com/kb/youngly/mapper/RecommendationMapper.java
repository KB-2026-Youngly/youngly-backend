package com.kb.youngly.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kb.youngly.dto.recommendation.PensionForecastFacts;
import com.kb.youngly.dto.recommendation.RecommendationResponse;
import com.kb.youngly.enums.Baseline;
import com.kb.youngly.vo.user.RecommendationVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RecommendationMapper {
    int insert(RecommendationVO recommendation);

    RecommendationVO selectLatestByUserId(@Param("userId") String userId);

    ObjectMapper JSON_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    static RecommendationVO toVO(String userId, String baseline, RecommendationResponse response) {
        try {
            return RecommendationVO.builder()
                    .userId(userId)
                    .baseline(Baseline.valueOf(baseline))
                    .periodStartDate(response.periodStartDate())
                    .periodEndDate(response.periodEndDate())
                    .settledAmountThisMonth(response.settledAmountThisMonth())
                    .expectedAdditionalAmountCurrentRank(response.expectedAdditionalAmountCurrentRank())
                    .expectedAdditionalAmountBestCase(response.expectedAdditionalAmountBestCase())
                    .expectedTotalAmountThisMonth(response.expectedTotalAmountThisMonth())
                    .expectedMaxTotalAmountThisMonth(response.expectedMaxTotalAmountThisMonth())
                    .currentPensionBalance(response.currentPensionBalance())
                    .hasOngoingRoundThisMonth(response.hasOngoingRoundThisMonth())
                    .nextDepositDate(response.nextDepositDate())
                    .forecastBasisJson(JSON_MAPPER.writeValueAsString(response.forecastBasis()))
                    .marketHighlightsJson(JSON_MAPPER.writeValueAsString(response.marketHighlights()))
                    .marketDetail(response.marketDetail())
                    .pensionInsightIntro(response.pensionInsightIntro())
                    .pensionInsightStrategy(response.pensionInsightStrategy())
                    .guardrailStatus(response.guardrailStatus())
                    .generationMode(response.generationMode())
                    .build();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("추천 응답 JSON 직렬화에 실패했습니다.", e);
        }
    }

    static RecommendationResponse toResponse(RecommendationVO vo) {
        try {
            List<PensionForecastFacts.GroupContribution> forecastBasis = vo.getForecastBasisJson() == null
                    ? List.of()
                    : JSON_MAPPER.readValue(vo.getForecastBasisJson(), new TypeReference<>() {});
            List<String> marketHighlights = vo.getMarketHighlightsJson() == null
                    ? List.of()
                    : JSON_MAPPER.readValue(vo.getMarketHighlightsJson(), new TypeReference<>() {});

            return new RecommendationResponse(
                    vo.getUserId(),
                    vo.getPeriodStartDate(),
                    vo.getPeriodEndDate(),
                    vo.getSettledAmountThisMonth(),
                    vo.getExpectedAdditionalAmountCurrentRank(),
                    vo.getExpectedAdditionalAmountBestCase(),
                    vo.getExpectedTotalAmountThisMonth(),
                    vo.getExpectedMaxTotalAmountThisMonth(),
                    vo.getCurrentPensionBalance(),
                    Boolean.TRUE.equals(vo.getHasOngoingRoundThisMonth()),
                    vo.getNextDepositDate(),
                    forecastBasis,
                    marketHighlights,
                    vo.getMarketDetail(),
                    vo.getPensionInsightIntro(),
                    vo.getPensionInsightStrategy(),
                    vo.getGenerationMode(),
                    vo.getGuardrailStatus()
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("추천 응답 JSON 역직렬화에 실패했습니다.", e);
        }
    }
}
