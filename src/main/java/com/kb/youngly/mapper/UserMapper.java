package com.kb.youngly.mapper;

import com.kb.youngly.vo.user.UserVO;
import com.kb.youngly.dto.auth.InterestOptionDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserMapper {

    int existsLoginId(@Param("loginId") String loginId);

    int existsEmail(@Param("email") String email);

    void insertUser(UserVO userVO);

    UserVO findByLoginId(@Param("loginId") String loginId);

    UserVO findByUserId(@Param("userId") String userId);

    void insertUserInterests(@Param("userId") String userId,
                             @Param("interestIds") List<Long> interestIds);

    List<InterestOptionDTO> selectInterestsByUserId(@Param("userId") String userId);

    void deactivateUser(String userId);
    void updatePassword(UserVO user);
    void updateUser(UserVO userVO);
}
