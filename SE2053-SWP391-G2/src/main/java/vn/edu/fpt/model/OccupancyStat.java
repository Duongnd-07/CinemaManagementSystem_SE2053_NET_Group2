package vn.edu.fpt.model;

import java.math.BigDecimal;

// UC-29: số vé bán và sức chứa của một phòng chiếu hoặc một phim trong kỳ báo cáo
public class OccupancyStat {
    private String name;
    private int showtimeCount;
    private int ticketCount;
    private int capacity;
    private BigDecimal ticketRevenue = BigDecimal.ZERO;
    private String tier;

    public OccupancyStat() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getShowtimeCount() {
        return showtimeCount;
    }

    public void setShowtimeCount(int showtimeCount) {
        this.showtimeCount = showtimeCount;
    }

    public int getTicketCount() {
        return ticketCount;
    }

    public void setTicketCount(int ticketCount) {
        this.ticketCount = ticketCount;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public BigDecimal getTicketRevenue() {
        return ticketRevenue;
    }

    public void setTicketRevenue(BigDecimal ticketRevenue) {
        this.ticketRevenue = ticketRevenue;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    // Tỷ lệ lấp đầy (%), làm tròn 1 chữ số; 0 khi chưa có sức chứa
    public double getOccupancyPercent() {
        return capacity <= 0 ? 0 : Math.round(ticketCount * 1000.0 / capacity) / 10.0;
    }
}
