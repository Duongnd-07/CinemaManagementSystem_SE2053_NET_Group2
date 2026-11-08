package vn.edu.fpt.service;

public class AnnouncementForm {
    private String target = "";
    private String phone = "";
    private String type = "";
    private String title = "";
    private String message = "";

    public String getTarget() { return target; }
    public void setTarget(String value) { target = trim(value); }
    public String getPhone() { return phone; }
    public void setPhone(String value) { phone = trim(value); }
    public String getType() { return type; }
    public void setType(String value) { type = trim(value); }
    public String getTitle() { return title; }
    public void setTitle(String value) { title = trim(value); }
    public String getMessage() { return message; }
    // Trình duyệt gửi xuống dòng dạng CRLF nhưng bộ đếm/maxlength phía client tính là 1 ký tự
    public void setMessage(String value) { message = trim(value == null ? null : value.replace("\r\n", "\n")); }

    private String trim(String value) { return value == null ? "" : value.trim(); }
}
