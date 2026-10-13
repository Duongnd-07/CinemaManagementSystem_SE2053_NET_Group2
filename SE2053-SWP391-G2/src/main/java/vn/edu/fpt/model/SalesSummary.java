package vn.edu.fpt.model;

import java.math.BigDecimal;

// UC-29: tổng hợp bán hàng của một kỳ báo cáo
public class SalesSummary {
    private int orderCount;
    private int ticketCount;
    private BigDecimal revenue = BigDecimal.ZERO;
    private BigDecimal concessionRevenue = BigDecimal.ZERO;

    public SalesSummary() {
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
