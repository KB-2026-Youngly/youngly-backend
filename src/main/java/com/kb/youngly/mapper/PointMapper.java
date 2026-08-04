package com.kb.youngly.mapper;

import com.kb.youngly.vo.point.PointHistoryVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 사용자 포인트와 포인트 사용 내역을 데이터베이스에서 조회·변경하는 MyBatis 매퍼입니다.
 * 각 메서드는 PointMapper.xml에 작성된 같은 이름의 SQL과 연결됩니다.
 */
public interface PointMapper {

    /**
     * 사용자의 현재 보유 포인트를 조회합니다.
     * 사용자가 존재하지 않으면 null을 반환합니다.
     */
    Long findUserPoint(@Param("userId") String userId);

    /**
     * 사용자의 보유 포인트에 지정한 포인트를 더합니다.
     * 정상적으로 변경되면 1, 사용자가 없으면 0을 반환합니다.
     */
    int increaseUserPoint(@Param("userId") String userId,
                          @Param("amount") int amount);

    /**
     * 사용자의 보유 포인트에서 지정한 포인트를 차감합니다.
     * 현재 포인트가 차감할 포인트보다 적으면 변경하지 않습니다.
     * 정상적으로 변경되면 1, 사용자 또는 잔액이 부족하면 0을 반환합니다.
     */
    int decreaseUserPoint(@Param("userId") String userId,
                          @Param("amount") int amount);

    /**
     * 포인트 적립 또는 사용 내역을 저장합니다.
     * 저장 후 DB에서 생성된 내역 ID가 pointHistory의 pointLogId에 설정됩니다.
     * 저장에 성공하면 저장된 행의 수를 반환합니다.
     */
    int insertPointHistory(PointHistoryVO pointHistory);

    /**
     * 사용자의 포인트 내역을 최신순으로 나누어 조회합니다.
     * limit은 한 번에 가져올 최대 개수이고, offset은 처음부터 건너뛸 개수입니다.
     */
    List<PointHistoryVO> findPointHistoriesByUserId(@Param("userId") String userId,
                                                    @Param("limit") int limit,
                                                    @Param("offset") int offset);
}
