package vn.edu.fpt.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// UC-20: các phương án xử lý cho một vé trên ghế hỏng, tính theo ghế còn trống của suất chiếu
public class ExchangeOptions {
    private final ExchangeTicket ticket;
    final List<Choice> same = new ArrayList<>();
    final List<Choice> upgrade = new ArrayList<>();
    final List<Choice> downgrade = new ArrayList<>();
    final List<Choice> pairs = new ArrayList<>();
    BigDecimal voucherValue = BigDecimal.ZERO;
    // Hoàn vé: phần khách thực trả cho vé, tách thành điểm trả lại + tiền mặt
    BigDecimal refundCash = BigDecimal.ZERO;
    int refundPoints;
    // Tiền mặt đền bù cố định khi xuống hạng hoặc tách ghế đôi
    BigDecimal compensationCash = BigDecimal.ZERO;

    public ExchangeOptions(ExchangeTicket ticket) {
        this.ticket = ticket;
    }

    // Một lựa chọn ghế: value gửi lên server ("12" hoặc "12-13" với 2 ghế), label hiển thị ("D5" / "D5 + D6")
    public static final class Choice {
        private final String value;
        private final String label;
        private final String seatType;

        public Choice(String value, String label, String seatType) {
            this.value = value;
            this.label = label;
            this.seatType = seatType;
        }

        public String getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }

        public String getSeatType() {
            return seatType;
        }
    }

    public ExchangeTicket getTicket() {
        return ticket;
    }

    public List<Choice> getSame() {
        return same;
    }

    public List<Choice> getUpgrade() {
        return upgrade;
    }

    public List<Choice> getDowngrade() {
        return downgrade;
    }

    public List<Choice> getPairs() {
        return pairs;
    }

    // Không còn ghế nào để đổi: chỉ còn cách hoàn tiền
    public boolean isNoSeatLeft() {
        return same.isEmpty() && upgrade.isEmpty() && downgrade.isEmpty() && pairs.isEmpty();
    }

    public BigDecimal getVoucherValue() {
        return voucherValue;
    }

    // Khách thành viên nhận voucher đền bù trong mọi trường hợp: đổi cùng hạng, nâng/xuống hạng, 2 ghế hoặc hoàn tiền
    public boolean isVoucherEligible() {
        return ticket.isMember() && voucherValue.signum() > 0;
    }

    public BigDecimal getRefundCash() {
        return refundCash;
    }

    public int getRefundPoints() {
        return refundPoints;
    }

    public BigDecimal getCompensationCash() {
        return compensationCash;
    }
}
