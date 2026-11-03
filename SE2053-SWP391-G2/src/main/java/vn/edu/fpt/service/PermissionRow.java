package vn.edu.fpt.service;

import java.util.List;
import vn.edu.fpt.model.Permission;

// UC-31: một dòng trên ma trận phân quyền = một chức năng + một ô cho mỗi vai trò
public class PermissionRow {
    private final Permission permission;
    private final List<Cell> cells;

    public PermissionRow(Permission permission, List<Cell> cells) {
        this.permission = permission;
        this.cells = cells;
    }

    public Permission getPermission() {
        return permission;
    }

    public List<Cell> getCells() {
        return cells;
    }

    public static class Cell {
        private final String role;
        private final String key;
        private final boolean assignable;
        private final boolean locked;
        private final boolean granted;

        public Cell(String role, String key, boolean assignable, boolean locked, boolean granted) {
            this.role = role;
            this.key = key;
            this.assignable = assignable;
            this.locked = locked;
            this.granted = granted;
        }

        public String getRole() {
            return role;
        }

        public String getKey() {
            return key;
        }

        public boolean isAssignable() {
            return assignable;
        }

        public boolean isLocked() {
            return locked;
        }

        public boolean isGranted() {
            return granted;
        }
    }
}
