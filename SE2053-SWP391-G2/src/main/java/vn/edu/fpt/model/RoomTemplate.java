package vn.edu.fpt.model;

import java.time.LocalDateTime;

// UC-25 (A3-A5): phòng mẫu - sơ đồ ghế dùng lại khi tạo phòng mới, không bao giờ là một phòng chiếu.
// layout cùng định dạng với RoomLayoutForm; seatCount/capacity tính từ layout khi đọc danh sách.
public class RoomTemplate {
    private int templateId;
    private String templateName;
    private String format;
    private int rows;
    private int cols;
    private String layout;
    private LocalDateTime updatedAt;
    private int seatCount;
    private int capacity;

    public int getTemplateId() {
        return templateId;
    }

    public void setTemplateId(int templateId) {
        this.templateId = templateId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public int getCols() {
        return cols;
    }

    public void setCols(int cols) {
        this.cols = cols;
    }

    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getSeatCount() {
        return seatCount;
    }

    public void setSeatCount(int seatCount) {
        this.seatCount = seatCount;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
}
