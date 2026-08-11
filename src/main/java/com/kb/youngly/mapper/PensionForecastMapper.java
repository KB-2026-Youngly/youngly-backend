package com.kb.youngly.mapper;

import com.kb.youngly.dto.recommendation.PensionForecastSource;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PensionForecastMapper {
    List<PensionForecastSource> findOngoingForecastSources(@Param("userId") String userId);

    BigDecimal sumConfirmedPensionSettlementThisMonth(@Param("userId") String userId);

    BigDecimal findPensionBalance(@Param("userId") String userId);
}
