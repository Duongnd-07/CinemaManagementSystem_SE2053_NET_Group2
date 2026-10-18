package vn.edu.fpt.service;

import java.math.BigDecimal;

// UC-13: một ghế trong đơn đặt vé; ticketCode/qrCode chỉ có sau khi đã tạo vé
public class BookingSeatLine {
    private final int seatId;
    private final String label;
    private final String seatType;
    private final BigDecimal price;
    private final String ticketCode;
    private final String qrCode;

    public BookingSeatLine(int seatId, String label, String seatType, BigDecimal price) {
        this(seatId, label, seatType, price, null, null);
    }

    public BookingSeatLine(int seatId, String label, String seatType, BigDecimal price, String ticketCode,
                           String qrCode) {
        this.seatId = seatId;
        this.label = label;
        this.seatType = seatType;
        this.price = price;
        this.ticketCode = ticketCode;
        this.qrCode = qrCode;
    }

    public int getSeatId() {
        return seatId;
    }

    public String getLabel() {
        return label;
    }

    public String getSeatType() {
        return seatType;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public String getQrCode() {
        return qrCode;
    }
}
