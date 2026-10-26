package vn.edu.fpt.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// UC-19: dữ liệu nhân viên nhập ở màn hình bán tại quầy, giữ nguyên chuỗi để hiển thị lại khi có lỗi
public class CounterSaleForm {
    private int showtimeId;
    private List<String> seatIds = new ArrayList<>();
    // ConcessionID -> số lượng nhập
    private Map<Integer, String> quantities = new LinkedHashMap<>();
    // Số điện thoại hoặc email khách thành viên; trống = khách vãng lai
    private String customer = "";
    private String voucherCode = "";
    private String points = "";
    private String paymentMethod = "";
    private String cashReceived = "";
    // Thẻ: mã chuẩn chi trên hóa đơn POS; chuyển khoản: mã giao dịch ngân hàng
    private String paymentReference = "";
    // Nội dung khách ghi khi chuyển khoản để nhân viên tìm đúng giao dịch trên app ngân hàng; giữ nguyên qua các lần
    // Tính tiền của cùng một đơn nhờ ô ẩn trên form
    private String transferNote = newTransferNote();
    private boolean ageConfirmed;

    private static final String NOTE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private static String newTransferNote() {
        StringBuilder note = new StringBuilder("CINE");
        for (int i = 0; i < 6; i++) {
            note.append(NOTE_CHARS.charAt(RANDOM.nextInt(NOTE_CHARS.length())));
        }
        return note.toString();
    }

    public int getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(int showtimeId) {
        this.showtimeId = showtimeId;
    }

    public List<String> getSeatIds() {
        return seatIds;
    }

    public void setSeatIds(List<String> seatIds) {
        this.seatIds = seatIds;
    }

    public Map<Integer, String> getQuantities() {
        return quantities;
    }

    public void setQuantities(Map<Integer, String> quantities) {
        this.quantities = quantities;
    }

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }

    public String getPoints() {
        return points;
    }

    public void setPoints(String points) {
        this.points = points;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getCashReceived() {
        return cashReceived;
    }

    public void setCashReceived(String cashReceived) {
        this.cashReceived = cashReceived;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getTransferNote() {
        return transferNote;
    }

    // Chỉ nhận giá trị đúng dạng do server tạo; trống hoặc sai thì giữ mã mới
    public void setTransferNote(String transferNote) {
        if (transferNote != null && transferNote.matches("CINE[A-Z0-9]{6}")) {
            this.transferNote = transferNote;
        }
    }

    public boolean isAgeConfirmed() {
        return ageConfirmed;
    }

    public void setAgeConfirmed(boolean ageConfirmed) {
        this.ageConfirmed = ageConfirmed;
    }
}
