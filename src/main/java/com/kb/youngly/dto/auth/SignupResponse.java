package com.kb.youngly.dto.auth;

public class SignupResponse {

    private String userId;

    private String loginId;

    private String nickname;

    public SignupResponse(String userId, String loginId, String nickname) {
        this.userId = userId;
        this.loginId = loginId;
        this.nickname = nickname;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}