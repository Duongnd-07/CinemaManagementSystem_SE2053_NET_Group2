package vn.edu.fpt.model;

import java.util.List;

// UC-17: trạng thái đánh giá phim; Hidden do Manager ẩn vì vi phạm
public final class ReviewStatus {
    public static final String VISIBLE = "Visible";
    public static final String HIDDEN = "Hidden";

    public static final List<String> ALL = List.of(VISIBLE, HIDDEN);

    private ReviewStatus() {
    }

    public static boolean isValid(String status) {
        return ALL.contains(status);
    }
}
