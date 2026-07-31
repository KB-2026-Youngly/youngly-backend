package com.kb.youngly.service;

import com.kb.youngly.dto.common.MessageResponse;
import com.kb.youngly.dto.user.UserResponse;

public interface UserService {

    UserResponse getMyInfo(String userId);

    MessageResponse deleteUser(String userId);
}