package com.kb.youngly.mapper;

import com.kb.youngly.vo.survey.InterestVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface InterestMapper {

    List<InterestVO> findUserInterestsByInvestment(@Param("userId") String userId,
                                                   @Param("investment") boolean investment);
}
