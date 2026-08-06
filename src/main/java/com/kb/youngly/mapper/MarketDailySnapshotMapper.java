package com.kb.youngly.mapper;

import com.kb.youngly.vo.market.MarketDailySnapshotVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface MarketDailySnapshotMapper {

    int upsertMarketDailySnapshot(MarketDailySnapshotVO snapshot);

    MarketDailySnapshotVO findByPdfFileName(@Param("pdfFileName") String pdfFileName);

    List<MarketDailySnapshotVO> findRecentSnapshots(@Param("from") LocalDate from,
                                                    @Param("to") LocalDate to,
                                                    @Param("limit") int limit);

    int deleteByPdfFileName(@Param("pdfFileName") String pdfFileName);
}
