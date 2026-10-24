package vn.edu.fpt.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import vn.edu.fpt.dao.SystemConfigManagementDAO;
import vn.edu.fpt.model.SystemConfig;
import vn.edu.fpt.util.DBContext;
import vn.edu.fpt.util.Messages;

// UC-33: Manage System Configurations
public class SystemConfigService {
    public static final String GENERAL_ERROR = "general";
    // Chỉ nhận số viết thường (không dấu phẩy nghìn, không dạng 1e3)
    private static final Pattern NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?");
    private static final int MAX_INPUT = 20;

    private final SystemConfigManagementDAO configDAO = new SystemConfigManagementDAO();

    public List<SystemConfigItem> loadItems() throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return toItems(configDAO.findAll(conn));
        }
    }

    // Gom theo nhóm để trang hiển thị từng khối (thứ tự theo SystemConfigParam)
    public Map<SystemConfigParam.Group, List<SystemConfigItem>> groupItems(List<SystemConfigItem> items) {
        Map<SystemConfigParam.Group, List<SystemConfigItem>> groups = new LinkedHashMap<>();
        for (SystemConfigItem item : items) {
            groups.computeIfAbsent(item.getParam().getGroup(), g -> new ArrayList<>()).add(item);
        }
        return groups;
    }

    // E1: hiển thị lại đúng giá trị Admin vừa nhập
    public void applyInput(List<SystemConfigItem> items, Map<String, String> input) {
        for (SystemConfigItem item : items) {
            String raw = input.get(item.getKey());
            item.setValue(raw == null ? "" : raw.trim());
        }
    }

    // Trả về các tham số đã đổi (khóa -> "cũ -> mới"); rỗng = không có thay đổi (A1)
    public Map<String, String> save(Map<String, String> input) throws ValidationException, SQLException {
        Map<SystemConfigParam, BigDecimal> values = validate(input);
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Map<String, SystemConfig> current = configDAO.findAll(conn, true);
                Map<String, String> changes = new LinkedHashMap<>();
                for (Map.Entry<SystemConfigParam, BigDecimal> entry : values.entrySet()) {
                    SystemConfigParam param = entry.getKey();
                    String newValue = normalize(param, entry.getValue());
                    SystemConfig stored = current.get(param.getKey());
                    if (stored == null) {
                        configDAO.insert(conn, param.getKey(), newValue, param.getHint());
                    } else if (!sameValue(stored.getConfigValue(), entry.getValue())) {
                        configDAO.update(conn, param.getKey(), newValue);
                    } else {
                        continue;
                    }
                    // BR-33-04: Servlet ghi từng thay đổi vào System Activity Log (UC-32) sau khi commit
                    changes.put(param.getKey(),
                            (stored == null ? "(chưa có)" : stored.getConfigValue()) + " -> " + newValue);
                }
                conn.commit();
                return changes;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private Map<SystemConfigParam, BigDecimal> validate(Map<String, String> input) throws ValidationException {
        Map<String, String> errors = new LinkedHashMap<>();
        Map<SystemConfigParam, BigDecimal> values = new LinkedHashMap<>();
        for (SystemConfigParam param : SystemConfigParam.values()) {
            String raw = input.get(param.getKey());
            raw = raw == null ? "" : raw.trim();
            String error = checkValue(param, raw);
            if (error != null) {
                errors.put(param.getKey(), error);
            } else {
                values.put(param, new BigDecimal(raw));
            }
        }
        // BR-33-03: ngưỡng bán chạy phải lớn hơn ngưỡng bán chậm
        BigDecimal top = values.get(SystemConfigParam.TOP_MOVIE_OCCUPANCY_PERCENT);
        BigDecimal low = values.get(SystemConfigParam.LOW_MOVIE_OCCUPANCY_PERCENT);
        if (top != null && low != null && top.compareTo(low) <= 0) {
            errors.put(SystemConfigParam.TOP_MOVIE_OCCUPANCY_PERCENT.getKey(), Messages.CONFIG_OCCUPANCY_ORDER);
        }
        if (!errors.isEmpty()) {
            errors.put(GENERAL_ERROR, Messages.CONFIG_INVALID);
            throw new ValidationException(errors);
        }
        return values;
    }

    private String checkValue(SystemConfigParam param, String raw) {
        if (raw.isEmpty()) {
            return Messages.format(Messages.MSG02, param.getLabel());
        }
        if (raw.length() > MAX_INPUT || !NUMBER.matcher(raw).matches()) {
            return Messages.format(Messages.CONFIG_NOT_NUMBER, param.getLabel());
        }
        BigDecimal value = new BigDecimal(raw);
        int scale = Math.max(0, value.stripTrailingZeros().scale());
        if (!param.isDecimal() && scale > 0) {
            return Messages.format(Messages.CONFIG_NOT_INTEGER, param.getLabel());
        }
        if (scale > SystemConfigParam.MAX_DECIMALS) {
            return Messages.format(Messages.CONFIG_TOO_MANY_DECIMALS, param.getLabel(), SystemConfigParam.MAX_DECIMALS);
        }
        if (value.compareTo(param.getMin()) < 0 || value.compareTo(param.getMax()) > 0) {
            return Messages.format(Messages.CONFIG_OUT_OF_RANGE, param.getLabel(),
                    param.getMin().toPlainString(), param.getMax().toPlainString(), param.getUnit());
        }
        return null;
    }

    private List<SystemConfigItem> toItems(Map<String, SystemConfig> stored) {
        List<SystemConfigItem> items = new ArrayList<>();
        for (SystemConfigParam param : SystemConfigParam.values()) {
            SystemConfig config = stored.get(param.getKey());
            if (config == null) {
                items.add(new SystemConfigItem(param, param.getDefaultValue(), param.getHint(), null));
            } else {
                String description = config.getDescription() == null || config.getDescription().isBlank()
                        ? param.getHint() : config.getDescription();
                items.add(new SystemConfigItem(param, config.getConfigValue(), description, config.getUpdatedAt()));
            }
        }
        return items;
    }

    // Lưu dạng gọn để các UC khác parse được: số nguyên không có ".0", số lẻ bỏ số 0 thừa
    private String normalize(SystemConfigParam param, BigDecimal value) {
        BigDecimal plain = value.stripTrailingZeros();
        if (!param.isDecimal() || plain.scale() <= 0) {
            return plain.toBigInteger().toString();
        }
        return plain.toPlainString();
    }

    // Giá trị cũ không phải số (bị sửa tay trong DB) thì coi như có thay đổi để ghi đè
    private boolean sameValue(String storedValue, BigDecimal newValue) {
        if (storedValue == null || !NUMBER.matcher(storedValue.trim()).matches()) {
            return false;
        }
        return new BigDecimal(storedValue.trim()).compareTo(newValue) == 0;
    }
}
