package vn.edu.fpt.service;

// UC-25: dữ liệu thô từ form phòng chiếu để Service validate và hiển thị lại khi có lỗi.
// layout: các hàng (A, B, ...) ngăn bởi "/", mỗi ký tự là một cột:
//   S / V / C = ghế Thường / VIP / Sweetbox đang dùng được, s / v / c = cùng loại nhưng bị khóa, "." = lối đi (không có ghế)
public class RoomLayoutForm {
    private int roomId;
    private String name = "";
    private String format = "";
    private String rows = "";
    private String cols = "";
    private String layout = "";

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getRows() {
        return rows;
    }

    public void setRows(String rows) {
        this.rows = rows;
    }

    public String getCols() {
        return cols;
    }

    public void setCols(String cols) {
        this.cols = cols;
    }

    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
    }
}
