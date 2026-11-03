package vn.edu.fpt.service;

import java.io.Serializable;

// UC-03: Dữ liệu thô từ form yêu cầu quên mật khẩu
public class ForgotPasswordForm implements Serializable {
    private static final long serialVersionUID = 1L;

    private String email = "";

    public ForgotPasswordForm() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? "" : email.trim();
    }
}
