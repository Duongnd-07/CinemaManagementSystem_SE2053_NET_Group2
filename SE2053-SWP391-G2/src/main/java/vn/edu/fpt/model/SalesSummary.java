package vn.edu.fpt.model;

import java.math.BigDecimal;

// UC-29: tổng hợp bán hàng của một kỳ báo cáo
public class SalesSummary {
    private int orderCount;
    private int ticketCount;
    private BigDecimal revenue = BigDecimal.ZERO;
    private BigDecimal concessionRevenue = BigDecimal.ZERO;
    // Phần của kênh online; tại quầy = tổng - online
    private int onlineOrderCount;
    private BigDecimal onlineRevenue = BigDecimal.ZERO;

    public SalesSummary() {
    }

    public int getOnlineOrderCount() {
        return onlineOrderCount;
    }

    public void setOnlineOrderCount(int onlineOrderCount) {
        this.onlineOrderCount = onlineOrderCount;
    }

    public BigDecimal getOnlineRevenue() {
        return onlineRevenue;
    }

    public void setOnlineRevenue(BigDecimal onlineRevenue) {
        this.onlineRevenue = onlineRevenue;
    }

    public int getCounterOrderCount() {
        return orderCount - onlineOrderCount;
    }

    public BigDecimal getCounterRevenue() {
        return revenue.subtract(onlineRevenue);
    }

    public int getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(int orderCount) {
        this.orderCount = orderCount;
    }

    public int getTicketCount() {
        return ticketCount;
    }

    public void setTicketCount(int ticketCount) {
        this.ticketCount = ticketCount;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public BigDecimal getConcessionRevenue() {
        return concessionRevenue;
    }

    public void setConcessionRevenue(BigDecimal concessionRevenue) {
        this.concessionRevenue = concessionRevenue;
    }
}
