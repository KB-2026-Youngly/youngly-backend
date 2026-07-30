package com.kb.youngly.mapper;

import org.apache.ibatis.annotations.Param;

import com.kb.youngly.vo.UserVO;

public interface UserMapper {

    int existsLoginId(@Param("loginId") String loginId);

    int existsEmail(@Param("email") String email);

    void insertUser(UserVO userVO);

}