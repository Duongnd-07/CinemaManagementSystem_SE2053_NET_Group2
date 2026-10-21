package vn.edu.fpt.service;

import java.io.Serializable;

// UC-03: Dữ liệu thô từ form đặt lại mật khẩu mới
public class ResetPasswordForm implements Serializable {
    private static final long serialVersionUID = 1L;

    private String token = "";
    private String newPassword = "";
    private String confirmPassword = "";

    public ResetPasswordForm() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token == null ? "" : token.trim();
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword == null ? "" : newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword == null ? "" : confirmPassword;
    }
}
