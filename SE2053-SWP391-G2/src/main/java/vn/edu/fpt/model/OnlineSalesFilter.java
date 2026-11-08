package vn.edu.fpt.model;

import java.time.LocalDate;

// UC-37: report data only; calculations belong to OnlineSalesReportService/DAO.
public class OnlineSalesFilter {
    private LocalDate from;
    private LocalDate to;
    private String groupBy = "day";
    private int page;

    public LocalDate getFrom() { return from; }
    public void setFrom(LocalDate from) { this.from = from; }

    public LocalDate getTo() { return to; }
    public void setTo(LocalDate to) { this.to = to; }

    public String getGroupBy() { return groupBy; }
    public void setGroupBy(String groupBy) { this.groupBy = groupBy; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
}

