package vn.edu.fpt.model;

import java.math.BigDecimal;

// UC-37: report data only; calculations belong to OnlineSalesReportService/DAO.
public class OnlineSalesSummary {
    private int orderCount;
    private int ticketCount;
    private BigDecimal ticketValue = BigDecimal.ZERO;
    private BigDecimal concessionValue = BigDecimal.ZERO;
    private BigDecimal collected = BigDecimal.ZERO;
    private BigDecimal grossAmount = BigDecimal.ZERO;

    public int getOrderCount() { return orderCount; }
    public void setOrderCount(int orderCount) { this.orderCount = orderCount; }

    public int getTicketCount() { return ticketCount; }
    public void setTicketCount(int ticketCount) { this.ticketCount = ticketCount; }

    public BigDecimal getTicketValue() { return ticketValue; }
    public void setTicketValue(BigDecimal ticketValue) { this.ticketValue = ticketValue; }

    public BigDecimal getConcessionValue() { return concessionValue; }
    public void setConcessionValue(BigDecimal concessionValue) { this.concessionValue = concessionValue; }

    public BigDecimal getCollected() { return collected; }
    public void setCollected(BigDecimal collected) { this.collected = collected; }

    public BigDecimal getGrossAmount() { return grossAmount; }
    public void setGrossAmount(BigDecimal grossAmount) { this.grossAmount = grossAmount; }
}

