package com.kb.youngly.mapper;

import com.kb.youngly.vo.UserVO;
import org.apache.ibatis.annotations.Param;

public interface UserMapper {

    int existsLoginId(@Param("loginId") String loginId);

    int existsEmail(@Param("email") String email);

    void insertUser(UserVO userVO);

    UserVO findByLoginId(@Param("loginId") String loginId);

    UserVO findByUserId(@Param("userId") String userId);

    void updatePassword(UserVO user);
}