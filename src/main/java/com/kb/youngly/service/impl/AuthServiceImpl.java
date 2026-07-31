package com.kb.youngly.service.impl;

import java.util.UUID;

import com.kb.youngly.dto.auth.LoginRequest;
import com.kb.youngly.dto.auth.LoginResponse;
import com.kb.youngly.dto.auth.SignupResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.kb.youngly.dto.auth.SignupRequest;
import com.kb.youngly.mapper.UserMapper;
import com.kb.youngly.service.AuthService;
import com.kb.youngly.vo.UserVO;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserMapper userMapper,
                           BCryptPasswordEncoder passwordEncoder) {

        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public SignupResponse signup(SignupRequest signupRequest) {

        // 로그인 아이디 중복 검사
        if (userMapper.existsLoginId(signupRequest.getLoginId()) > 0) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        // 이메일 중복 검사
        if (userMapper.existsEmail(signupRequest.getEmail()) > 0) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        UserVO userVO = new UserVO();

        // UUID 생성
        userVO.setUserId(UUID.randomUUID().toString());

        userVO.setLoginId(signupRequest.getLoginId());

        // 비밀번호 암호화
        userVO.setPassword(
                passwordEncoder.encode(signupRequest.getPassword())
        );

        userVO.setName(signupRequest.getName());
        userVO.setNickname(signupRequest.getNickname());
        userVO.setEmail(signupRequest.getEmail());

        // 기본값
        userVO.setProfileImageUrl(null);
//        userVO.setUserStatus("ACTIVE");
//        userVO.setPoint(0L);

        // 회원 저장
        userMapper.insertUser(userVO);

//        SignupResponse response = new SignupResponse();
//        response.setUserId(userVO.getUserId());
//        response.setLoginId(userVO.getLoginId());
//        response.setNickname(userVO.getNickname());
//
//        return response;
        return new SignupResponse(
                userVO.getUserId(),
                userVO.getLoginId(),
                userVO.getNickname()
        );
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {

        UserVO user = userMapper.findByLoginId(loginRequest.getLoginId());

        if (user == null) {
            throw new IllegalArgumentException("존재하지 않는 아이디입니다.");
        }

        if (!passwordEncoder.matches(
                loginRequest.getPassword(),
                user.getPassword())) {

            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return new LoginResponse(
                user.getUserId(),
                user.getLoginId(),
                user.getNickname()
        );
    }
}