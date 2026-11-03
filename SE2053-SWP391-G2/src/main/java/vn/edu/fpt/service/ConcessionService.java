package vn.edu.fpt.service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import vn.edu.fpt.dao.ConcessionDAO;
import vn.edu.fpt.model.Concession;
import vn.edu.fpt.model.ConcessionStatus;
import vn.edu.fpt.util.Messages;

public class ConcessionService {
    public static final int PAGE_SIZE = 8;
    // Form chỉ cho chọn hai trạng thái này; Out of Stock do hệ thống tự đặt theo tồn kho
    public static final List<String> FORM_STATUSES = List.of(ConcessionStatus.ACTIVE, ConcessionStatus.HIDDEN);

    private static final int MAX_NAME = 100;
    private static final int MAX_DESCRIPTION = 500;
    private static final int MAX_URL = 500;
    private static final Pattern PRICE = Pattern.compile("\\d{1,9}");
    private static final Pattern STOCK = Pattern.compile("\\d{1,6}");
    private static final int SQL_UNIQUE_VIOLATION = 2627;
    private static final int SQL_UNIQUE_INDEX_VIOLATION = 2601;

    private final ConcessionDAO concessionDAO = new ConcessionDAO();

    public ConcessionPage list(String keyword, String status, int page) throws SQLException {
        String kw = keyword == null ? "" : keyword.trim();
        String statusFilter = ConcessionStatus.isValid(status) ? status : null;
        int total = concessionDAO.count(kw, statusFilter);
        int totalPages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        int currentPage = Math.min(Math.max(page, 1), totalPages);
        List<Concession> concessions = concessionDAO.search(kw, statusFilter, (currentPage - 1) * PAGE_SIZE,
                PAGE_SIZE);
        return new ConcessionPage(concessions, currentPage, PAGE_SIZE, total, concessionDAO.countByStatus(kw));
    }

    public Concession findById(int concessionId) throws SQLException {
        return concessionDAO.findById(concessionId);
    }

    public List<Concession> findAvailableForSale() throws SQLException {
        return concessionDAO.findAvailableForSale();
    }

    public ConcessionForm toForm(Concession concession) {
        ConcessionForm form = new ConcessionForm();
        form.setConcessionId(concession.getConcessionId());
        form.setName(nullToEmpty(concession.getName()));
        form.setDescription(nullToEmpty(concession.getDescription()));
        form.setPrice(concession.getPrice() == null ? "" : concession.getPrice().stripTrailingZeros().toPlainString());
        form.setStockQuantity(String.valueOf(concession.getStockQuantity()));
        form.setImageUrl(nullToEmpty(concession.getImageUrl()));
        form.setStatus(ConcessionStatus.HIDDEN.equals(concession.getStatus())
                ? ConcessionStatus.HIDDEN : ConcessionStatus.ACTIVE);
        return form;
    }

    // E1: trả về toàn bộ lỗi theo từng trường; map rỗng nghĩa là hợp lệ
    public Map<String, String> validate(ConcessionForm form) throws SQLException {
        Map<String, String> errors = new LinkedHashMap<>();

        String name = form.getName().trim();
        if (name.isEmpty()) {
            errors.put("name", Messages.format(Messages.MSG02, "Tên món"));
        } else if (name.length() > MAX_NAME) {
            errors.put("name", Messages.format(Messages.MSG08, MAX_NAME));
        } else if (concessionDAO.existsByName(name, form.getConcessionId())) {
            errors.put("name", Messages.CONCESSION_NAME_DUPLICATE);
        }

        if (form.getDescription().trim().length() > MAX_DESCRIPTION) {
            errors.put("description", Messages.format(Messages.MSG08, MAX_DESCRIPTION));
        }

        // GB-10: giá và tồn kho không âm
        String price = form.getPrice().trim();
        if (price.isEmpty()) {
            errors.put("price", Messages.format(Messages.MSG02, "Giá bán"));
        } else if (!PRICE.matcher(price).matches()) {
            errors.put("price", Messages.CONCESSION_PRICE_INVALID);
        }
        String stock = form.getStockQuantity().trim();
        if (stock.isEmpty()) {
            errors.put("stockQuantity", Messages.format(Messages.MSG02, "Tồn kho"));
        } else if (!STOCK.matcher(stock).matches()) {
            errors.put("stockQuantity", Messages.CONCESSION_STOCK_INVALID);
        }

        String imageUrl = form.getImageUrl().trim();
        if (imageUrl.length() > MAX_URL) {
            errors.put("imageUrl", Messages.format(Messages.MSG08, MAX_URL));
        } else if (!imageUrl.isEmpty() && !isHttpUrl(imageUrl)) {
            errors.put("imageUrl", Messages.CONCESSION_IMAGE_URL_INVALID);
        }

        if (!FORM_STATUSES.contains(form.getStatus())) {
            errors.put("status", Messages.CONCESSION_STATUS_INVALID);
        }
        return errors;
    }

    // Trả về true nếu là thêm mới, false nếu là cập nhật
    public boolean save(ConcessionForm form) throws SQLException, ValidationException {
        Map<String, String> errors = validate(form);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        boolean isNew = form.getConcessionId() <= 0;
        if (!isNew && concessionDAO.findById(form.getConcessionId()) == null) {
            throw new ValidationException(Map.of("general", Messages.CONCESSION_NOT_FOUND));
        }
        Concession concession = toConcession(form);
        try {
            if (isNew) {
                concessionDAO.insert(concession);
            } else {
                concessionDAO.update(concession);
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == SQL_UNIQUE_VIOLATION || e.getErrorCode() == SQL_UNIQUE_INDEX_VIOLATION) {
                throw new ValidationException(Map.of("name", Messages.CONCESSION_NAME_DUPLICATE));
            }
            throw e;
        }
        return isNew;
    }

    // GB-12: không xóa cứng món đã có trong đơn hàng, chỉ chuyển sang Hidden
    public void hide(int concessionId) throws SQLException, ValidationException {
        if (concessionDAO.findById(concessionId) == null) {
            throw new ValidationException(Map.of("general", Messages.CONCESSION_NOT_FOUND));
        }
        concessionDAO.updateStatus(concessionId, ConcessionStatus.HIDDEN);
    }

    private Concession toConcession(ConcessionForm form) {
        Concession concession = new Concession();
        concession.setConcessionId(form.getConcessionId());
        concession.setName(form.getName().trim());
        concession.setDescription(blankToNull(form.getDescription()));
        concession.setPrice(new BigDecimal(form.getPrice().trim()));
        concession.setStockQuantity(Integer.parseInt(form.getStockQuantity().trim()));
        concession.setImageUrl(blankToNull(form.getImageUrl()));
        concession.setStatus(resolveStatus(form.getStatus(), concession.getStockQuantity()));
        return concession;
    }

    // GB-10: hết hàng thì Out of Stock, có hàng trở lại thì Active; món đang ẩn vẫn giữ Hidden
    private String resolveStatus(String chosenStatus, int stockQuantity) {
        if (ConcessionStatus.HIDDEN.equals(chosenStatus)) {
            return ConcessionStatus.HIDDEN;
        }
        return stockQuantity == 0 ? ConcessionStatus.OUT_OF_STOCK : ConcessionStatus.ACTIVE;
    }

    private boolean isHttpUrl(String value) {
        try {
            URI uri = new URI(value);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
