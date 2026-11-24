package vn.edu.fpt.model;

import java.io.Serializable;

// UC-36 (A7): kết quả một lần quét vé ở cửa phòng chiếu, hiện thành ô xanh (cho vào) hoặc đỏ (từ chối).
// suspicious = vé đã vào cửa rồi hoặc vé giấy cũ / mã in không hợp lệ (dấu hiệu vé photo), được ghi cảnh báo UC-32.
public class CheckInResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final boolean admitted;
    private final String message;
    private final String ticketCode;
    private final String seatLabel;
    private final String detail;
    private final int bookingId;
    private final boolean suspicious;

    public CheckInResult(boolean admitted, String message, String ticketCode, String seatLabel, String detail,
            int bookingId, boolean suspicious) {
        this.admitted = admitted;
        this.message = message;
        this.ticketCode = ticketCode;
        this.seatLabel = seatLabel;
        this.detail = detail;
        this.bookingId = bookingId;
        this.suspicious = suspicious;
    }

    public boolean isAdmitted() {
        return admitted;
    }

    public String getMessage() {
        return message;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public String getSeatLabel() {
        return seatLabel;
    }

    public String getDetail() {
        return detail;
    }

    public int getBookingId() {
        return bookingId;
    }

    public boolean isSuspicious() {
        return suspicious;
    }
}
