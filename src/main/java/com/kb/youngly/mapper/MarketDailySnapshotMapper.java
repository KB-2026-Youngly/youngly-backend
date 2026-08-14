package com.kb.youngly.mapper;

import com.kb.youngly.vo.market.MarketDailySnapshotVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface MarketDailySnapshotMapper {

    int upsertMarketDailySnapshot(MarketDailySnapshotVO snapshot);

    MarketDailySnapshotVO findByPdfFileName(@Param("pdfFileName") String pdfFileName);

    MarketDailySnapshotVO findByMarketDateAndSourceSubject(@Param("marketDate") LocalDate marketDate,
                                                           @Param("sourceSubject") String sourceSubject);

    List<MarketDailySnapshotVO> findRecentSnapshots(@Param("from") LocalDate from,
                                                    @Param("to") LocalDate to,
                                                    @Param("limit") int limit);

    List<MarketDailySnapshotVO> findSnapshotsByMarketDate(@Param("marketDate") LocalDate marketDate);

    MarketDailySnapshotVO findLatestSnapshotWithMarketSummary(@Param("from") LocalDate from,
                                                              @Param("to") LocalDate to);

    int updateMarketSummary(@Param("id") Long id,
                            @Param("marketHeadlineText") String marketHeadlineText,
                            @Param("marketDetailText") String marketDetailText);

    int updateMarketSummaryIfAbsent(@Param("id") Long id,
                                    @Param("marketHeadlineText") String marketHeadlineText,
                                    @Param("marketDetailText") String marketDetailText);

    int deleteByPdfFileName(@Param("pdfFileName") String pdfFileName);

    int deleteByMarketDateAndSourceSubject(@Param("marketDate") LocalDate marketDate,
                                           @Param("sourceSubject") String sourceSubject);
}
