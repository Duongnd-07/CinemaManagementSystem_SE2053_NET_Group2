package vn.edu.fpt.model;

import java.util.List;

public final class ReportStatus {
    public static final String PENDING = "Pending";
    public static final String IN_PROGRESS = "In Progress";
    public static final String RESOLVED = "Resolved";

    // Theo đúng thứ tự vòng đời, UC-26 chỉ cho đi tới
    public static final List<String> ALL = List.of(PENDING, IN_PROGRESS, RESOLVED);

    private ReportStatus() {
    }

    public static boolean isValid(String status) {
        return ALL.contains(status);
    }
}
