package vn.edu.fpt.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// UC-33: một dòng trên trang cấu hình = định nghĩa tham số + giá trị đang lưu (hoặc giá trị Admin vừa nhập khi lỗi)
public class SystemConfigItem {
    private static final DateTimeFormatter UPDATED_FORMAT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final SystemConfigParam param;
    private final String storedValue;
    private final String description;
    private final LocalDateTime updatedAt;
    private String value;

    public SystemConfigItem(SystemConfigParam param, String storedValue, String description, LocalDateTime updatedAt) {
        this.param = param;
        this.storedValue = storedValue;
        this.description = description;
        this.updatedAt = updatedAt;
        this.value = storedValue;
    }

    public SystemConfigParam getParam() {
        return param;
    }

    public String getKey() {
        return param.getKey();
    }

    // Giá trị hiển thị trong ô nhập
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getStoredValue() {
        return storedValue;
    }

    public String getDescription() {
        return description;
    }

    // Chưa có dòng trong DB: các UC đang dùng giá trị mặc định trong code
    public boolean isStored() {
        return updatedAt != null;
    }

    public String getUpdatedAtText() {
        return updatedAt == null ? "" : updatedAt.format(UPDATED_FORMAT);
    }
}
