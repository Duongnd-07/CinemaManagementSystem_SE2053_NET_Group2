package vn.edu.fpt.service;

// UC-09: một ghế trên sơ đồ, status là một trong bốn trạng thái của SeatStatus (GB-02)
public class SeatMapCell {
    private final int seatId;
    private final String rowName;
    private final int colNumber;
    private final String seatType;
    private final String status;

    public SeatMapCell(int seatId, String rowName, int colNumber, String seatType, String status) {
        this.seatId = seatId;
        this.rowName = rowName;
        this.colNumber = colNumber;
        this.seatType = seatType;
        this.status = status;
    }

    public int getSeatId() {
        return seatId;
    }

    public String getRowName() {
        return rowName;
    }

    public int getColNumber() {
        return colNumber;
    }

    public String getSeatType() {
        return seatType;
    }

    public String getStatus() {
        return status;
    }

    public String getLabel() {
        return rowName + colNumber;
    }
}
