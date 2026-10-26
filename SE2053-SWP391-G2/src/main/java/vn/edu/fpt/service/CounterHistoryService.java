package vn.edu.fpt.service;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import vn.edu.fpt.dao.CounterHistoryDAO;
import vn.edu.fpt.dao.ShiftDAO;
import vn.edu.fpt.model.Shift;
import vn.edu.fpt.util.Page;

// Lịch sử bán tại quầy (UC-19). Staff: đơn trong từng ca của chính mình; Manager: đơn của mọi nhân viên theo ngày
public class CounterHistoryService {
    public static final int PAGE_SIZE = 15;

    private final CounterHistoryDAO historyDAO = new CounterHistoryDAO();
    private final ShiftDAO shiftDAO = new ShiftDAO();

    // Kết quả một trang lịch sử kèm tổng của mọi đơn khớp bộ lọc
    public static final class Result {
        private final Page<CounterOrder> page;
        private final int orderCount;
        private final BigDecimal collected;
        private final BigDecimal refunded;

        Result(Page<CounterOrder> page, BigDecimal[] totals) {
            this.page = page;
            this.orderCount = totals[0].intValue();
            this.collected = totals[1];
            this.refunded = totals[2];
        }

        public Page<CounterOrder> getPage() {
            return page;
        }

        public int getOrderCount() {
            return orderCount;
        }

        public BigDecimal getCollected() {
            return collected;
        }

        public BigDecimal getRefunded() {
            return refunded;
        }

        // Thực thu sau khi trừ tiền đã hoàn cho khách
        public BigDecimal getNet() {
            return collected.subtract(refunded);
        }
    }

    // Các ca gần đây của nhân viên để chọn xem lịch sử
    public List<Shift> listShifts(int staffId) throws SQLException {
        return shiftDAO.findRecentByStaff(staffId);
    }

    // Ca được xem: ca yêu cầu nếu là của nhân viên, không thì ca đang mở / gần nhất; null nếu chưa có ca nào
    public Shift resolveShift(List<Shift> shifts, int requestedShiftId) {
        for (Shift shift : shifts) {
            if (shift.getShiftId() == requestedShiftId) {
                return shift;
            }
        }
        return shifts.isEmpty() ? null : shifts.get(0);
    }

    public Result listForStaff(int staffId, int shiftId, String keyword, int page) throws SQLException {
        return list(new CounterHistoryDAO.Filter().staff(staffId).shift(shiftId).keyword(keyword), page);
    }

    public Result listForManager(LocalDate date, String keyword, int page) throws SQLException {
        return list(new CounterHistoryDAO.Filter()
                .period(date.atStartOfDay(), date.plusDays(1).atStartOfDay()).keyword(keyword), page);
    }

    private Result list(CounterHistoryDAO.Filter filter, int page) throws SQLException {
        BigDecimal[] totals = historyDAO.summarize(filter);
        Page<CounterOrder> orders = Page.of(page, PAGE_SIZE, totals[0].intValue(),
                (offset, limit) -> historyDAO.search(filter, offset, limit));
        return new Result(orders, totals);
    }
}
