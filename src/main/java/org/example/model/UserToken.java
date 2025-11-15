package org.example.model;

public class UserToken {
    //a
    private String phone;
    private String accessToken;
    private String refreshToken;

    public UserToken(String phone, String accessToken, String refreshToken) {
        this.phone = phone;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public String getPhone() {
        return phone;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
