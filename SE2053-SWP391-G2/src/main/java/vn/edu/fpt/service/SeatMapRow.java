package vn.edu.fpt.service;

import java.util.List;

// UC-09: một hàng ghế; phần tử null là vị trí không có ghế (lối đi)
public class SeatMapRow {
    private final String rowName;
    private final List<SeatMapCell> cells;

    public SeatMapRow(String rowName, List<SeatMapCell> cells) {
        this.rowName = rowName;
        this.cells = cells;
    }

    public String getRowName() {
        return rowName;
    }

    public List<SeatMapCell> getCells() {
        return cells;
    }
}
