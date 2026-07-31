package com.kb.youngly.service;

import com.kb.youngly.dto.auth.LoginRequest;
import com.kb.youngly.dto.auth.LoginResponse;
import com.kb.youngly.dto.auth.SignupRequest;
import com.kb.youngly.dto.auth.SignupResponse;

public interface AuthService {

    SignupResponse signup(SignupRequest signupRequest);

    LoginResponse login(LoginRequest loginRequest);
}