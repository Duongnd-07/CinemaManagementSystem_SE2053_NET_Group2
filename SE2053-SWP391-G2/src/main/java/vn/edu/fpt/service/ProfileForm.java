package vn.edu.fpt.service;

// UC-10: Dữ liệu thô từ form Thông tin cá nhân (Email đổi riêng qua luồng xác thực OTP)
public class ProfileForm {
    private String fullName = "";
    private String phone = "";
    // Chuỗi yyyy-MM-dd từ input type=date; giữ dạng thô để hiển thị lại khi có lỗi
    private String dateOfBirth = "";
    private String gender = "";

    public ProfileForm() {
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = FullNameRule.normalize(fullName);
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone == null ? "" : phone.trim();
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth == null ? "" : dateOfBirth.trim();
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender == null ? "" : gender.trim();
    }
}
