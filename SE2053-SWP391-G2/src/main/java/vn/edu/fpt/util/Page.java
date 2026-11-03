package vn.edu.fpt.util;

import java.sql.SQLException;
import java.util.List;

/**
 * Kết quả phân trang dùng chung cho mọi danh sách.
 * Dùng {@link #of} ở Service, hiển thị bằng tag /WEB-INF/tags/pagination.tag ở JSP.
 */
public class Page<T> {

    /** Nạp dữ liệu của một trang, DAO chỉ cần nhận offset/limit. */
    @FunctionalInterface
    public interface Loader<T> {
        List<T> load(int offset, int limit) throws SQLException;
    }

    private final List<T> items;
    private final int page;
    private final int pageSize;
    private final int totalItems;

    public Page(List<T> items, int page, int pageSize, int totalItems) {
        this.items = items;
        this.page = page;
        this.pageSize = pageSize;
        this.totalItems = totalItems;
    }

    // Trang yêu cầu nằm ngoài khoảng hợp lệ sẽ được đưa về [1, tổng số trang]
    public static <T> Page<T> of(int requestedPage, int pageSize, int totalItems, Loader<T> loader)
            throws SQLException {
        int totalPages = Math.max(1, (totalItems + pageSize - 1) / pageSize);
        int currentPage = Math.min(Math.max(requestedPage, 1), totalPages);
        List<T> items = loader.load((currentPage - 1) * pageSize, pageSize);
        return new Page<>(items, currentPage, pageSize, totalItems);
    }

    public List<T> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getTotalPages() {
        return Math.max(1, (totalItems + pageSize - 1) / pageSize);
    }

    public int getFromItem() {
        return totalItems == 0 ? 0 : (page - 1) * pageSize + 1;
    }

    public int getToItem() {
        return Math.min(totalItems, page * pageSize);
    }
}
