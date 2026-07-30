package com.kb.youngly.mapper;

import com.kb.youngly.vo.PointHistoryVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PointMapper {
    Long findUserPoint(@Param("userId") String userId);

    int increaseUserPoint(@Param("userId") String userId,
                          @Param("amount") int amount);

    int decreaseUserPoint(@Param("userId") String userId,
                          @Param("amount") int amount);

    int insertPointHistory(PointHistoryVO pointHistory);

    List<PointHistoryVO> findPointHistoriesByUserId(@Param("userId") String userId,
                                                    @Param("limit") int limit,
                                                    @Param("offset") int offset);
}
