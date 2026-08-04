package com.kb.youngly.service;

import com.kb.youngly.dto.auth.*;

public interface AuthService {

    SignupResponse signup(SignupRequest signupRequest);

    LoginResponse login(LoginRequest loginRequest);

    LogoutResponse logout();
}