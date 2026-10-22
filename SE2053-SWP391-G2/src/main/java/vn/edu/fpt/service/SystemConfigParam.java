package vn.edu.fpt.service;

import java.math.BigDecimal;

// UC-33 BR-33-01/02: danh sách tham số Admin được sửa, kèm kiểu dữ liệu và khoảng hợp lệ.
// Admin chỉ sửa giá trị, không thêm/xóa/đổi tên key vì các UC khác đọc theo tên key.
// Phụ thu ghế VIP (SEAT_SURCHARGE_VIP) là giá vé do Manager quản lý nên không nằm ở đây.
public enum SystemConfigParam {
    SEAT_HOLD_MINUTES(Group.BOOKING, "Thời gian giữ ghế", "phút",
            "Thời gian giữ ghế khi khách đặt vé online, quá hạn chưa thanh toán thì nhả ghế.", "10", 1, 60, false),
    BOOKING_MAX_SEATS(Group.BOOKING, "Số ghế tối đa mỗi đơn", "ghế",
            "Số ghế khách được chọn trong một đơn đặt vé online.", "8", 1, 20, false),
    CANCELLATION_VOUCHER_DAYS(Group.VOUCHER, "Hiệu lực voucher hoàn vé", "ngày",
            "Số ngày hiệu lực của voucher hoàn 80% giá vé khi khách hủy vé online.", "30", 1, 365, false),
    BROKEN_SEAT_VOUCHER_VALUE(Group.VOUCHER, "Giá trị voucher đền bù ghế hỏng", "VND",
            "Giá trị voucher đền bù khi ghế hỏng và không còn ghế tương đương để đổi.", "50000", 0, 10_000_000, false),
    BROKEN_SEAT_VOUCHER_DAYS(Group.VOUCHER, "Hiệu lực voucher đền bù ghế hỏng", "ngày",
            "Số ngày hiệu lực của voucher đền bù ghế hỏng.", "30", 1, 365, false),
    POINT_EARN_PERCENT(Group.LOYALTY, "Tỷ lệ tích điểm", "%",
            "Phần trăm tiền vé (sau giảm giá) được quy thành điểm thưởng.", "1", 0, 100, true),
    POINT_EXCHANGE_RATE(Group.LOYALTY, "Giá trị quy đổi 1 điểm", "VND",
            "Số tiền được trừ vào giá vé cho mỗi điểm thưởng khách dùng.", "1000", 1, 1_000_000, false),
    LOW_MOVIE_OCCUPANCY_PERCENT(Group.REPORT, "Ngưỡng phim bán chậm", "%",
            "Phim có tỷ lệ lấp đầy dưới mức này được xếp là bán chậm.", "30", 0, 100, false),
    TOP_MOVIE_OCCUPANCY_PERCENT(Group.REPORT, "Ngưỡng phim bán chạy", "%",
            "Phim có tỷ lệ lấp đầy từ mức này trở lên được xếp là bán chạy.", "70", 0, 100, false);

    // Số chữ số thập phân tối đa của tham số cho phép số lẻ
    public static final int MAX_DECIMALS = 2;

    public enum Group {
        BOOKING("Đặt vé", "confirmation_number"),
        VOUCHER("Voucher", "redeem"),
        LOYALTY("Điểm thưởng", "loyalty"),
        REPORT("Báo cáo", "monitoring");

        private final String label;
        private final String icon;

        Group(String label, String icon) {
            this.label = label;
            this.icon = icon;
        }

        public String getLabel() {
            return label;
        }

        public String getIcon() {
            return icon;
        }
    }

    private final Group group;
    private final String label;
    private final String unit;
    private final String hint;
    private final String defaultValue;
    private final BigDecimal min;
    private final BigDecimal max;
    private final boolean decimal;

    SystemConfigParam(Group group, String label, String unit, String hint, String defaultValue,
            long min, long max, boolean decimal) {
        this.group = group;
        this.label = label;
        this.unit = unit;
        this.hint = hint;
        this.defaultValue = defaultValue;
        this.min = BigDecimal.valueOf(min);
        this.max = BigDecimal.valueOf(max);
        this.decimal = decimal;
    }

    public String getKey() {
        return name();
    }

    public Group getGroup() {
        return group;
    }

    public String getLabel() {
        return label;
    }

    public String getUnit() {
        return unit;
    }

    public String getHint() {
        return hint;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public BigDecimal getMin() {
        return min;
    }

    public BigDecimal getMax() {
        return max;
    }

    public boolean isDecimal() {
        return decimal;
    }

    // Bước nhập cho ô number trên form
    public String getStep() {
        return decimal ? "0.01" : "1";
    }
}
