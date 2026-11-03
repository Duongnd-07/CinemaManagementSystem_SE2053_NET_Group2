package vn.edu.fpt.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;

/**
 * UC-24: Thuật toán định giá vé theo ngày thường/cuối tuần và khung giờ,
 * cùng thuật toán tính giờ kết thúc (thời lượng + 15 phút dọn rạp - GB-08).
 */
public final class ShowtimePricing {

    // Phụ thu theo định dạng phòng chiếu
    public static final BigDecimal SURCHARGE_3D = new BigDecimal("20000");
    public static final BigDecimal SURCHARGE_IMAX = new BigDecimal("50000");

    // Bảng giá vé 2D ngày thường (Thứ 2 - Thứ 5)
    public static final BigDecimal WEEKDAY_MORNING = new BigDecimal("65000");    // < 12:00
    public static final BigDecimal WEEKDAY_AFTERNOON = new BigDecimal("75000");  // 12:00 - 16:59
    public static final BigDecimal WEEKDAY_PRIME = new BigDecimal("85000");      // 17:00 - 21:59 (Giờ vàng)
    public static final BigDecimal WEEKDAY_LATE = new BigDecimal("75000");       // >= 22:00

    // Bảng giá vé 2D cuối tuần (Thứ 6, Thứ 7, Chủ Nhật)
    public static final BigDecimal WEEKEND_MORNING = new BigDecimal("80000");    // < 12:00
    public static final BigDecimal WEEKEND_AFTERNOON = new BigDecimal("95000");  // 12:00 - 16:59
    public static final BigDecimal WEEKEND_PRIME = new BigDecimal("110000");     // 17:00 - 21:59 (Giờ vàng)
    public static final BigDecimal WEEKEND_LATE = new BigDecimal("90000");       // >= 22:00

    public static final int CLEANING_MINUTES = 15;

    private ShowtimePricing() {
    }

    /**
     * GB-08: Giờ kết thúc = Giờ bắt đầu + Thời lượng phim + 15 phút dọn phòng
     */
    public static LocalDateTime calculateEndTime(LocalDateTime startTime, int durationMinutes) {
        if (startTime == null || durationMinutes <= 0) {
            return null;
        }
        return startTime.plusMinutes(durationMinutes + (long) CLEANING_MINUTES);
    }

    /**
     * Kiểm tra xem ngày có phải cuối tuần (Thứ 6, Thứ 7, Chủ Nhật) hay không
     */
    public static boolean isWeekend(DayOfWeek day) {
        return day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    /**
     * Tính giá vé cơ bản gợi ý theo ngày thường/cuối tuần, khung giờ và định dạng phòng
     */
    public static BigDecimal calculateSuggestedPrice(LocalDateTime startTime, String roomFormat) {
        if (startTime == null) {
            return WEEKDAY_AFTERNOON;
        }

        boolean weekend = isWeekend(startTime.getDayOfWeek());
        int hour = startTime.getHour();

        BigDecimal basePrice;
        if (hour < 12) {
            basePrice = weekend ? WEEKEND_MORNING : WEEKDAY_MORNING;
        } else if (hour < 17) {
            basePrice = weekend ? WEEKEND_AFTERNOON : WEEKDAY_AFTERNOON;
        } else if (hour < 22) {
            basePrice = weekend ? WEEKEND_PRIME : WEEKDAY_PRIME;
        } else {
            basePrice = weekend ? WEEKEND_LATE : WEEKDAY_LATE;
        }

        BigDecimal surcharge = formatSurcharge(roomFormat);
        return basePrice.add(surcharge);
    }

    /**
     * Mô tả chi tiết cách tính giá để hiển thị trực quan cho Manager
     */
    public static String describePricing(LocalDateTime startTime, String roomFormat) {
        if (startTime == null) {
            return "Vui lòng chọn thời gian bắt đầu";
        }

        boolean weekend = isWeekend(startTime.getDayOfWeek());
        String dayName = switch (startTime.getDayOfWeek()) {
            case MONDAY -> "Thứ Hai";
            case TUESDAY -> "Thứ Ba";
            case WEDNESDAY -> "Thứ Tư";
            case THURSDAY -> "Thứ Năm";
            case FRIDAY -> "Thứ Sáu";
            case SATURDAY -> "Thứ Bảy";
            case SUNDAY -> "Chủ Nhật";
        };
        String dayType = weekend ? "Cuối tuần" : "Ngày thường";

        int hour = startTime.getHour();
        String slotName;
        BigDecimal slotPrice;
        if (hour < 12) {
            slotName = "Suất sáng (< 12h)";
            slotPrice = weekend ? WEEKEND_MORNING : WEEKDAY_MORNING;
        } else if (hour < 17) {
            slotName = "Suất chiều (12h - 17h)";
            slotPrice = weekend ? WEEKEND_AFTERNOON : WEEKDAY_AFTERNOON;
        } else if (hour < 22) {
            slotName = "Giờ vàng tối (17h - 22h)";
            slotPrice = weekend ? WEEKEND_PRIME : WEEKDAY_PRIME;
        } else {
            slotName = "Suất khuya (≥ 22h)";
            slotPrice = weekend ? WEEKEND_LATE : WEEKDAY_LATE;
        }

        BigDecimal surcharge = formatSurcharge(roomFormat);
        String formatNote = "";
        if (surcharge.compareTo(BigDecimal.ZERO) > 0) {
            formatNote = " · " + (roomFormat != null ? roomFormat : "Đặc biệt") + " (+" + formatMoney(surcharge) + ")";
        }

        BigDecimal total = slotPrice.add(surcharge);
        return dayName + " (" + dayType + ") · " + slotName + formatNote + " → Gợi ý: " + formatMoney(total);
    }

    public static BigDecimal formatSurcharge(String roomFormat) {
        if (roomFormat == null) {
            return BigDecimal.ZERO;
        }
        if ("3D".equalsIgnoreCase(roomFormat.trim())) {
            return SURCHARGE_3D;
        }
        if ("IMAX".equalsIgnoreCase(roomFormat.trim())) {
            return SURCHARGE_IMAX;
        }
        return BigDecimal.ZERO;
    }

    private static String formatMoney(BigDecimal amount) {
        if (amount == null) return "0đ";
        return String.format("%,dđ", amount.longValue()).replace(',', '.');
    }
}
