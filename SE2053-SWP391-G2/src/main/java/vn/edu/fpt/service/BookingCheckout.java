package vn.edu.fpt.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// UC-13: phiên đặt vé đang ở bước 6-8 (đã giữ ghế, đang chọn bắp nước/voucher/điểm), lưu trong HttpSession.
// Tiền không lưu ở đây: mỗi lần hiển thị/chốt đơn BookingService tính lại từ DB.
public class BookingCheckout implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String holdSessionId;
    private final int showtimeId;
    private final List<Integer> seatIds;
    private Map<Integer, Integer> concessions = new LinkedHashMap<>();
    private String voucherCode;
    private int pointsRedeemed;
    // Có giá trị sau khi bước 9 đã tạo đơn Pending (chặn bấm xác nhận hai lần)
    private Integer bookingId;

    public BookingCheckout(String holdSessionId, int showtimeId, List<Integer> seatIds) {
        this.holdSessionId = holdSessionId;
        this.showtimeId = showtimeId;
        this.seatIds = new ArrayList<>(seatIds);
    }

    public String getHoldSessionId() {
        return holdSessionId;
    }

    public int getShowtimeId() {
        return showtimeId;
    }

    public List<Integer> getSeatIds() {
        return seatIds;
    }

    public Map<Integer, Integer> getConcessions() {
        return concessions;
    }

    public void setConcessions(Map<Integer, Integer> concessions) {
        this.concessions = new LinkedHashMap<>(concessions);
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }

    public int getPointsRedeemed() {
        return pointsRedeemed;
    }

    public void setPointsRedeemed(int pointsRedeemed) {
        this.pointsRedeemed = pointsRedeemed;
    }

    public Integer getBookingId() {
        return bookingId;
    }

    public void setBookingId(Integer bookingId) {
        this.bookingId = bookingId;
    }
}
