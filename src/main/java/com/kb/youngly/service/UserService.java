package com.kb.youngly.service;

import com.kb.youngly.dto.common.MessageResponse;
import com.kb.youngly.dto.user.UpdatePasswordRequest;
import com.kb.youngly.dto.user.UpdateUserRequest;
import com.kb.youngly.dto.user.UserResponse;

public interface UserService {

    UserResponse getMyInfo(String userId);

    MessageResponse updatePassword(String userId,
                                   UpdatePasswordRequest request);
    MessageResponse updateUser(String userId, UpdateUserRequest request);
}