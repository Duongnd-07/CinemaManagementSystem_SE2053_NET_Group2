package vn.edu.fpt.service;

import java.math.BigDecimal;
import java.util.Map;

// UC-13 bước 1-4: sơ đồ ghế (dữ liệu UC-09) kèm giá từng loại ghế để khách chọn
public class BookingSeatView {
    private final SeatMapView seatMap;
    private final Map<String, BigDecimal> seatPrices;
    private final int maxSeats;
    private final boolean ageRestricted;

    public BookingSeatView(SeatMapView seatMap, Map<String, BigDecimal> seatPrices, int maxSeats,
                           boolean ageRestricted) {
        this.seatMap = seatMap;
        this.seatPrices = seatPrices;
        this.maxSeats = maxSeats;
        this.ageRestricted = ageRestricted;
    }

    public SeatMapView getSeatMap() {
        return seatMap;
    }

    public Map<String, BigDecimal> getSeatPrices() {
        return seatPrices;
    }

    public int getMaxSeats() {
        return maxSeats;
    }

    public boolean isAgeRestricted() {
        return ageRestricted;
    }
}
