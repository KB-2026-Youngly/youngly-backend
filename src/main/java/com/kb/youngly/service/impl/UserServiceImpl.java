package com.kb.youngly.service.impl;

import com.kb.youngly.dto.common.MessageResponse;
import com.kb.youngly.dto.user.UserResponse;
import com.kb.youngly.mapper.UserMapper;
import com.kb.youngly.service.UserService;
import com.kb.youngly.vo.UserVO;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserResponse getMyInfo(String userId) {

        UserVO user = userMapper.findByUserId(userId);

        return UserResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .loginId(user.getLoginId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .point(user.getPoint())
                .build();
    }

    @Override
    public MessageResponse deleteUser(String userId) {

        UserVO user = userMapper.findByUserId(userId);

        if (user == null) {
            throw new IllegalArgumentException("존재하지 않는 회원입니다.");
        }

        userMapper.deactivateUser(userId);

        return MessageResponse.builder()
                .message("Success")
                .build();
    }
}