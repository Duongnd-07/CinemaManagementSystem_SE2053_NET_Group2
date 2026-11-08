package vn.edu.fpt.model;

import java.util.List;

// UC-37: report data only; calculations belong to OnlineSalesReportService/DAO.
public class OnlineSalesDetail {
    private OnlineSalesRow order;
    private List<OnlineSalesRow> tickets;
    private List<OnlineSalesRow> concessions;

    public OnlineSalesRow getOrder() { return order; }
    public void setOrder(OnlineSalesRow order) { this.order = order; }

    public List<OnlineSalesRow> getTickets() { return tickets; }
    public void setTickets(List<OnlineSalesRow> tickets) { this.tickets = tickets; }

    public List<OnlineSalesRow> getConcessions() { return concessions; }
    public void setConcessions(List<OnlineSalesRow> concessions) { this.concessions = concessions; }


}

