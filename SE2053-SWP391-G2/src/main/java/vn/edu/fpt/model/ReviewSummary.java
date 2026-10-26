package vn.edu.fpt.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

// UC-17: điểm trung bình + phân bố số sao của một phim, chỉ tính review Status = Visible
public class ReviewSummary {
    private int totalReviews;
    private BigDecimal averageRating = BigDecimal.ZERO;
    private final Map<Integer, Integer> starCounts = new LinkedHashMap<>();

    public ReviewSummary() {
        for (int star = 5; star >= 1; star--) {
            starCounts.put(star, 0);
        }
    }

    // ratingCounts: số lượt theo từng mức sao (1-5), chưa chuẩn hóa thứ tự
    public static ReviewSummary of(Map<Integer, Integer> ratingCounts) {
        ReviewSummary summary = new ReviewSummary();
        int total = 0;
        long sum = 0;
        for (Map.Entry<Integer, Integer> entry : ratingCounts.entrySet()) {
            int star = entry.getKey();
            int count = entry.getValue();
            if (star >= 1 && star <= 5) {
                summary.starCounts.put(star, count);
                total += count;
                sum += (long) star * count;
            }
        }
        summary.totalReviews = total;
        summary.averageRating = total == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
        return summary;
    }

    public int getTotalReviews() {
        return totalReviews;
    }

    public BigDecimal getAverageRating() {
        return averageRating;
    }

    public Map<Integer, Integer> getStarCounts() {
        return starCounts;
    }

    // % số review ở mức sao này so với tổng, dùng vẽ thanh phân bố trên giao diện
    public int percentOf(int star) {
        if (totalReviews == 0) {
            return 0;
        }
        int count = starCounts.getOrDefault(star, 0);
        return (int) Math.round(count * 100.0 / totalReviews);
    }
}
