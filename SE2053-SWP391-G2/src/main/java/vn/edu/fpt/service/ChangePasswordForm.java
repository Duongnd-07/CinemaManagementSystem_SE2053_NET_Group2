package vn.edu.fpt.service;

// UC-10 AF1: Dữ liệu thô từ form Đổi mật khẩu; không trim vì khoảng trắng là một phần của mật khẩu
public class ChangePasswordForm {
    private String currentPassword = "";
    private String newPassword = "";
    private String confirmPassword = "";

    public ChangePasswordForm() {
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword == null ? "" : currentPassword;
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
