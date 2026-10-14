package vn.edu.fpt.service;

// UC-10: Dữ liệu thô từ form Thông tin cá nhân (Email không có ở đây vì không cho phép sửa - BR-PROF-01)
public class ProfileForm {
    private String fullName = "";
    private String phone = "";

    public ProfileForm() {
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName == null ? "" : fullName.trim();
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone == null ? "" : phone.trim();
    }
}
