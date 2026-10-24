package vn.edu.fpt.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import vn.edu.fpt.model.Seat;
import vn.edu.fpt.model.SeatType;

// UC-13 bước 4: không để trống một ghế lẻ ở giữa hoặc cạnh ghế khách chọn (giống CGV, Lotte).
// Mỗi hàng chia thành các dãy ghế liền nhau (lối đi/cột trống ngắt dãy). Trong dãy, các ghế còn trống TRƯỚC khi chọn
// tạo thành từng đoạn; ghế khách chọn chia đoạn thành các khoảng trống. Khoảng trống đúng một ghế đơn là ghế lẻ
// (một ghế đôi trống vẫn bán được cho 2 người nên không tính), trừ khi đoạn đó chỉ còn thừa đúng một ghế:
// khi đó chọn cách nào cũng thừa một ghế nên không chặn.
// assets/js/booking-seats.js cài cùng thuật toán để báo ngay trên trình duyệt; server kiểm tra lại khi giữ ghế.
public final class SeatGapRule {

    private SeatGapRule() {
    }

    // Nhãn ghế (VD "C5") bị bỏ lẻ đầu tiên, null nếu lựa chọn hợp lệ.
    // freeSeatIds: ghế còn trống trước khi chọn (gồm cả ghế đang chọn), selectedSeatIds: ghế khách chọn.
    public static String findOrphanSeat(Collection<Seat> roomSeats, Set<Integer> freeSeatIds,
                                        Set<Integer> selectedSeatIds) {
        Map<String, List<Seat>> rows = new LinkedHashMap<>();
        for (Seat seat : roomSeats) {
            rows.computeIfAbsent(seat.getRowName(), k -> new ArrayList<>()).add(seat);
        }
        for (List<Seat> row : rows.values()) {
            row.sort(Comparator.comparingInt(Seat::getColNumber));
            List<Seat> run = new ArrayList<>();
            Seat previous = null;
            for (Seat seat : row) {
                boolean free = freeSeatIds.contains(seat.getSeatId());
                boolean adjacent = previous != null
                        && previous.getColNumber() + SeatType.widthOf(previous.getSeatType()) == seat.getColNumber();
                if (!free || !adjacent) {
                    String orphan = orphanInRun(run, selectedSeatIds);
                    if (orphan != null) {
                        return orphan;
                    }
                    run.clear();
                }
                if (free) {
                    run.add(seat);
                }
                previous = seat;
            }
            String orphan = orphanInRun(run, selectedSeatIds);
            if (orphan != null) {
                return orphan;
            }
        }
        return null;
    }

    private static String orphanInRun(List<Seat> run, Set<Integer> selectedSeatIds) {
        int selectedCount = 0;
        for (Seat seat : run) {
            if (selectedSeatIds.contains(seat.getSeatId())) {
                selectedCount++;
            }
        }
        if (selectedCount == 0 || run.size() - selectedCount == 1) {
            return null;
        }
        List<Seat> gap = new ArrayList<>();
        for (Seat seat : run) {
            if (selectedSeatIds.contains(seat.getSeatId())) {
                if (isOrphan(gap)) {
                    return label(gap.get(0));
                }
                gap.clear();
            } else {
                gap.add(seat);
            }
        }
        return isOrphan(gap) ? label(gap.get(0)) : null;
    }

    private static boolean isOrphan(List<Seat> gap) {
        return gap.size() == 1 && SeatType.widthOf(gap.get(0).getSeatType()) == 1;
    }

    private static String label(Seat seat) {
        return seat.getRowName() + seat.getColNumber();
    }
}
