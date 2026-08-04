package com.kb.youngly.service.impl;

import com.kb.youngly.dto.common.MessageResponse;
import com.kb.youngly.dto.user.UpdatePasswordRequest;
import com.kb.youngly.dto.user.UpdateUserRequest;
import com.kb.youngly.dto.user.UserResponse;
import com.kb.youngly.mapper.UserMapper;
import com.kb.youngly.service.UserService;
import com.kb.youngly.vo.user.UserVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserMapper userMapper,
                           PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
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
                .birthday(user.getBirthday())
                .build();
    }

    @Override
    public MessageResponse updatePassword(String userId,
                                          UpdatePasswordRequest request) {

        UserVO user = userMapper.findByUserId(userId);

        if (user == null) {
            throw new IllegalArgumentException("존재하지 않는 회원입니다.");
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("기존 비밀번호가 일치하지 않습니다.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        userMapper.updatePassword(user);

        return MessageResponse.builder()
                .message("Success")
                .build();
    }

    @Override
    public MessageResponse updateUser(String userId,
                                      UpdateUserRequest request) {

        UserVO user = new UserVO();
        user.setUserId(userId);
        user.setNickname(request.getNickname());
        user.setProfileImageUrl(request.getProfileImageUrl());

        userMapper.updateUser(user);

        return MessageResponse.builder()
                .message("Success")
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