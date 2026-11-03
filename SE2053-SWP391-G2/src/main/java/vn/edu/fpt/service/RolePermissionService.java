package vn.edu.fpt.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import vn.edu.fpt.dao.RolePermissionDAO;
import vn.edu.fpt.model.Permission;
import vn.edu.fpt.util.DBContext;
import vn.edu.fpt.util.Messages;

// UC-31: Manage Roles & Permissions
public class RolePermissionService {
    // AuthFilter kiểm tra ở mọi request nên giữ bản đọc từ DB trong bộ nhớ. Lưu qua trang Admin có hiệu lực ngay;
    // sửa trực tiếp trong DB có hiệu lực khi bản này hết hạn.
    private static final long CACHE_TTL_NANOS = TimeUnit.SECONDS.toNanos(30);
    // Nạp và xóa đều synchronized: lần nạp đã đọc dữ liệu cũ luôn kết thúc trước khi bị xóa, không ghi đè bản mới.
    private static volatile CachedRoles cache;

    private final RolePermissionDAO permissionDAO = new RolePermissionDAO();

    public record Change(String role, Permission permission, boolean granted) {
    }

    private record CachedRoles(Map<String, Set<Permission>> roles, long loadedAt) {
        boolean isFresh() {
            return System.nanoTime() - loadedAt < CACHE_TTL_NANOS;
        }
    }

    public boolean isAllowed(String role, Permission permission) throws SQLException {
        return allowedFor(role).contains(permission);
    }

    public Set<Permission> allowedFor(String role) throws SQLException {
        return snapshot().getOrDefault(role, Set.of());
    }

    // Chức năng quản lý đầu tiên vai trò này được dùng; null nếu không có
    public String firstManagementPath(String role) throws SQLException {
        for (Permission permission : allowedFor(role)) {
            if (permission.getGroup() == Permission.Group.MANAGEMENT) {
                return permission.getPath();
            }
        }
        return null;
    }

    // Gom theo nhóm để trang hiển thị từng khối (thứ tự theo enum Permission)
    public Map<Permission.Group, List<PermissionRow>> loadRows() throws SQLException {
        Map<String, Boolean> stored;
        try (Connection conn = DBContext.getConnection()) {
            stored = permissionDAO.findAll(conn);
        }
        Map<Permission.Group, List<PermissionRow>> groups = new LinkedHashMap<>();
        for (Permission permission : Permission.values()) {
            List<PermissionRow.Cell> cells = new ArrayList<>();
            for (String role : Permission.CONFIGURABLE_ROLES) {
                cells.add(new PermissionRow.Cell(role, permission.keyFor(role), permission.isAssignableTo(role),
                        permission.isLockedFor(role), isGranted(stored, role, permission)));
            }
            groups.computeIfAbsent(permission.getGroup(), g -> new ArrayList<>())
                    .add(new PermissionRow(permission, cells));
        }
        return groups;
    }

    // Trả về các quyền đã đổi; rỗng = không có thay đổi. Ô bị khóa không gửi lên và luôn giữ nguyên.
    public List<Change> save(String[] submittedKeys) throws ValidationException, SQLException {
        Set<String> granted = validate(submittedKeys);
        List<Change> changes = new ArrayList<>();
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Map<String, Boolean> stored = permissionDAO.findAll(conn, true);
                for (Permission permission : Permission.values()) {
                    for (String role : Permission.CONFIGURABLE_ROLES) {
                        if (!permission.isAssignableTo(role) || permission.isLockedFor(role)) {
                            continue;
                        }
                        boolean newValue = granted.contains(permission.keyFor(role));
                        if (newValue != isGranted(stored, role, permission)) {
                            permissionDAO.save(conn, role, permission.name(), newValue);
                            changes.add(new Change(role, permission, newValue));
                        }
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
        if (!changes.isEmpty()) {
            clearCache();
        }
        return changes;
    }

    // Giao diện chỉ gửi ô hợp lệ; khóa lạ nghĩa là form bị sửa nên từ chối toàn bộ, không lưu gì
    private Set<String> validate(String[] submittedKeys) throws ValidationException {
        Set<String> valid = new HashSet<>();
        for (Permission permission : Permission.values()) {
            for (String role : Permission.CONFIGURABLE_ROLES) {
                if (permission.isAssignableTo(role)) {
                    valid.add(permission.keyFor(role));
                }
            }
        }
        Set<String> granted = new HashSet<>();
        if (submittedKeys != null) {
            for (String key : submittedKeys) {
                if (!valid.contains(key)) {
                    throw new ValidationException(Map.of("general", Messages.PERMISSION_INVALID));
                }
                granted.add(key);
            }
        }
        return granted;
    }

    private static boolean isGranted(Map<String, Boolean> stored, String role, Permission permission) {
        if (!permission.isAssignableTo(role)) {
            return false;
        }
        if (permission.isLockedFor(role)) {
            return true;
        }
        Boolean value = stored.get(permission.keyFor(role));
        return value == null ? permission.isDefaultFor(role) : value;
    }

    private Map<String, Set<Permission>> snapshot() throws SQLException {
        CachedRoles current = cache;
        return current != null && current.isFresh() ? current.roles() : loadCache();
    }

    private Map<String, Set<Permission>> loadCache() throws SQLException {
        synchronized (RolePermissionService.class) {
            if (cache == null || !cache.isFresh()) {
                Map<String, Boolean> stored;
                try (Connection conn = DBContext.getConnection()) {
                    stored = permissionDAO.findAll(conn);
                }
                Map<String, Set<Permission>> loaded = new HashMap<>();
                for (String role : Permission.CONFIGURABLE_ROLES) {
                    Set<Permission> allowed = EnumSet.noneOf(Permission.class);
                    for (Permission permission : Permission.values()) {
                        if (isGranted(stored, role, permission)) {
                            allowed.add(permission);
                        }
                    }
                    loaded.put(role, allowed);
                }
                cache = new CachedRoles(loaded, System.nanoTime());
            }
            return cache.roles();
        }
    }

    private static void clearCache() {
        synchronized (RolePermissionService.class) {
            cache = null;
        }
    }
}
