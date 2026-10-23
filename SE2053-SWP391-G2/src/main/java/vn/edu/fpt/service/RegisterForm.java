package vn.edu.fpt.service;

import java.io.Serializable;

// UC-01: Dữ liệu thô từ form đăng ký để Service validate và hiển thị lại trên JSP khi có lỗi
public class RegisterForm implements Serializable {
    private static final long serialVersionUID = 1L;

    private String fullName = "";
    private String email = "";
    private String phone = "";
    private String password = "";
    private String confirmPassword = "";
    // SRS UC-01: Checkbox đồng ý Điều khoản & Chính sách (Agreement)
    private boolean agreedTerms = false;

    public RegisterForm() {
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = FullNameRule.normalize(fullName);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? "" : email.trim();
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone == null ? "" : phone.trim();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password == null ? "" : password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword == null ? "" : confirmPassword;
    }

    public boolean isAgreedTerms() {
        return agreedTerms;
    }

    public void setAgreedTerms(boolean agreedTerms) {
        this.agreedTerms = agreedTerms;
    }
}
