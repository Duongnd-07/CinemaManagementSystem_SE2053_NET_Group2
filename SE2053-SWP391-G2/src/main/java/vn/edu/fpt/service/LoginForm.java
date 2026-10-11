package vn.edu.fpt.service;

import java.io.Serializable;

// UC-02: Dữ liệu thô từ form đăng nhập để Service validate và tái hiện khi có lỗi
public class LoginForm implements Serializable {
    private static final long serialVersionUID = 1L;

    private String identifier = ""; // Email hoặc Số điện thoại
    private String password = "";
    private boolean rememberMe = false;
    private String returnUrl = "";

    public LoginForm() {
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier == null ? "" : identifier.trim();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password == null ? "" : password;
    }

    public boolean isRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl == null ? "" : returnUrl.trim();
    }
}
