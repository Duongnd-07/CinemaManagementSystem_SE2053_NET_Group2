package vn.edu.fpt.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// UC-37: report data only; calculations belong to OnlineSalesReportService/DAO.
public class OnlineSalesResult {
    private OnlineSalesSummary summary;
    private List<OnlineSalesRow> rows;
    private Map<LocalDate, BigDecimal> collections;
    private int totalRows;
    private int page;
    private int pageCount;
    private LocalDateTime generatedAt;
    private BigDecimal maxCollection = BigDecimal.ZERO;

    public OnlineSalesSummary getSummary() { return summary; }
    public void setSummary(OnlineSalesSummary summary) { this.summary = summary; }

    public List<OnlineSalesRow> getRows() { return rows; }
    public void setRows(List<OnlineSalesRow> rows) { this.rows = rows; }

    public Map<LocalDate, BigDecimal> getCollections() { return collections; }
    public void setCollections(Map<LocalDate, BigDecimal> collections) { this.collections = collections; }

    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getPageCount() { return pageCount; }
    public void setPageCount(int pageCount) { this.pageCount = pageCount; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public BigDecimal getMaxCollection() { return maxCollection; }
    public void setMaxCollection(BigDecimal value) { maxCollection = value; }
}

