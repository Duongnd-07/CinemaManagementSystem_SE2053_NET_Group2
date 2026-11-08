package vn.edu.fpt.model;

import java.util.List;
import java.util.Set;

// UC-31: danh mục chức năng Admin được phân quyền, mỗi chức năng ứng với đúng một URL.
// Chỉ phân quyền cho Manager và Staff: khu vực /admin/* luôn dành cho Admin, /customer/* luôn dành cho Customer.
// Trang đích sau đăng nhập không được thu hồi: /staff/dashboard không nằm trong danh mục, Quản lý phim bị khóa với Manager.
public enum Permission {
    MANAGE_MOVIES(Group.MANAGEMENT, "Quản lý phim", "Thêm, sửa và ngừng chiếu phim.", "/manager/movies"),
    MANAGE_GENRES(Group.MANAGEMENT, "Quản lý thể loại phim", "Thêm, sửa và ẩn thể loại phim.", "/manager/genres"),
    MANAGE_SHOWTIMES(Group.MANAGEMENT, "Quản lý suất chiếu", "Tạo, sửa và hủy suất chiếu.", "/manager/showtimes"),
    MANAGE_ROOMS(Group.MANAGEMENT, "Quản lý phòng chiếu", "Quản lý phòng, sơ đồ ghế và thiết bị.", "/manager/rooms"),
    MANAGE_ROOM_TEMPLATES(Group.MANAGEMENT, "Quản lý phòng mẫu", "Tạo và sửa sơ đồ phòng mẫu.", "/manager/room-templates"),
    MANAGE_CONCESSIONS(Group.MANAGEMENT, "Quản lý đồ ăn & thức uống", "Quản lý danh mục và giá bắp nước.", "/manager/concessions"),
    MANAGE_VOUCHERS(Group.MANAGEMENT, "Quản lý voucher & khuyến mãi", "Tạo và quản lý voucher.", "/manager/vouchers"),
    MANAGE_EQUIPMENT_REPORTS(Group.MANAGEMENT, "Xử lý sự cố thiết bị", "Tiếp nhận và cập nhật sự cố do nhân viên báo.", "/manager/equipment-reports"),
    VIEW_ROOM_INSPECTIONS(Group.MANAGEMENT, "Xem lịch sử kiểm tra phòng", "Xem kết quả kiểm tra và dọn phòng theo ngày.", "/manager/room-inspections"),
    MANAGE_REVIEWS(Group.MANAGEMENT, "Kiểm duyệt đánh giá phim", "Duyệt hoặc ẩn đánh giá của khách.", "/manager/reviews"),
    SEND_ANNOUNCEMENTS(Group.MANAGEMENT, "Gửi thông báo", "Soạn, gửi và thu hồi thông báo.", "/manager/announcements"),
    VIEW_SHIFT_REPORTS(Group.MANAGEMENT, "Xem báo cáo ca", "Xem báo cáo của mọi ca bán hàng.", "/manager/shifts"),
    VIEW_COUNTER_HISTORY(Group.MANAGEMENT, "Xem lịch sử bán tại quầy", "Xem đơn bán tại quầy của mọi nhân viên.", "/manager/counter-history"),
    VIEW_REPORTS(Group.MANAGEMENT, "Xem báo cáo thống kê", "Xem doanh thu và thống kê phim.", "/manager/reports"),
    VIEW_ONLINE_REPORTS(Group.MANAGEMENT, "Xem lịch sử bán vé online",
            "Xem đơn online đã thanh toán và thống kê tiền thu theo ngày.", "/manager/online-reports"),

    COUNTER_SALE(Group.OPERATION, "Bán vé tại quầy", "Mở ca và bán vé, bắp nước tại quầy.", "/staff/counter"),
    OWN_COUNTER_HISTORY(Group.OPERATION, "Lịch sử bán vé của mình", "Xem các đơn do chính nhân viên đã bán.", "/staff/counter-history"),
    OWN_SHIFTS(Group.OPERATION, "Ca làm việc", "Xem báo cáo và đóng ca của chính nhân viên.", "/staff/shifts"),
    SEAT_EXCHANGE(Group.OPERATION, "Đổi ghế hỏng", "Đổi ghế hoặc đền bù voucher khi ghế hỏng.", "/staff/seat-exchange"),
    REPORT_EQUIPMENT(Group.OPERATION, "Báo sự cố thiết bị", "Ghi nhận sự cố thiết bị trong phòng chiếu.", "/staff/equipment-reports"),
    ROOM_INSPECTION(Group.OPERATION, "Kiểm tra và dọn phòng chiếu", "Kiểm tra và dọn phòng trước mỗi suất chiếu.", "/staff/room-inspections"),
    TICKET_PRINT(Group.OPERATION, "Soát và in vé", "Quét vé điện tử và in vé giấy.", "/staff/ticket-print");

    // Thứ tự cột trên trang phân quyền
    public static final List<String> CONFIGURABLE_ROLES = List.of(Role.MANAGER, Role.STAFF);

    public enum Group {
        // Staff có thể được giao thêm các chức năng quản lý; mặc định chỉ Manager có
        MANAGEMENT("Nghiệp vụ quản lý", "manage_accounts", Set.of(Role.MANAGER, Role.STAFF), Set.of(Role.MANAGER)),
        // Servlet /staff/* chỉ phục vụ Staff (SRS: Primary Actor Staff) nên không giao cho Manager
        OPERATION("Nghiệp vụ nhân viên", "badge", Set.of(Role.STAFF), Set.of(Role.STAFF));

        private final String label;
        private final String icon;
        private final Set<String> assignableRoles;
        private final Set<String> defaultRoles;

        Group(String label, String icon, Set<String> assignableRoles, Set<String> defaultRoles) {
            this.label = label;
            this.icon = icon;
            this.assignableRoles = assignableRoles;
            this.defaultRoles = defaultRoles;
        }

        public String getLabel() {
            return label;
        }

        public String getIcon() {
            return icon;
        }
    }

    private final Group group;
    private final String label;
    private final String description;
    private final String path;

    Permission(Group group, String label, String description, String path) {
        this.group = group;
        this.label = label;
        this.description = description;
        this.path = path;
    }

    // null: URL không thuộc danh mục, AuthFilter giữ quy tắc theo tiền tố
    public static Permission fromPath(String servletPath) {
        for (Permission permission : values()) {
            if (permission.path.equals(servletPath)) {
                return permission;
            }
        }
        return null;
    }

    public boolean isAssignableTo(String role) {
        return group.assignableRoles.contains(role);
    }

    public boolean isDefaultFor(String role) {
        return group.defaultRoles.contains(role);
    }

    // Quyền luôn bật: /manager/movies là trang đích của Manager sau khi đăng nhập
    public boolean isLockedFor(String role) {
        return this == MANAGE_MOVIES && Role.MANAGER.equals(role);
    }

    // Định danh dùng làm giá trị ô tick và TargetID trong nhật ký
    public String keyFor(String role) {
        return role + ":" + name();
    }

    public Group getGroup() {
        return group;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public String getPath() {
        return path;
    }
}
