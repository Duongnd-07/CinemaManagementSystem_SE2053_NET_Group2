package vn.edu.fpt.service;

import java.math.BigDecimal;
import vn.edu.fpt.model.SeatType;

// Giá vé theo loại ghế, dùng chung cho đặt vé online (UC-13) và bán tại quầy (UC-19):
// - Ghế thường: giá suất chiếu
// - Ghế VIP: giá suất chiếu + phụ thu VIP (SystemConfig SEAT_SURCHARGE_VIP)
// - Ghế đôi (Sweetbox): giá của 2 ghế thường; một vé ghế đôi dành cho 2 người
public final class TicketPricing {

    private TicketPricing() {
    }

    public static BigDecimal priceOf(String seatType, BigDecimal basePrice, BigDecimal vipSurcharge) {
        if (SeatType.VIP.equals(seatType)) {
            return basePrice.add(vipSurcharge == null ? BigDecimal.ZERO : vipSurcharge);
        }
        return basePrice.multiply(BigDecimal.valueOf(SeatType.capacityOf(seatType)));
    }
}
