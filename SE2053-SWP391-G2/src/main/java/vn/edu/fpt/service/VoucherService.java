package vn.edu.fpt.service;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import vn.edu.fpt.dao.VoucherDAO;
import vn.edu.fpt.model.Voucher;
import vn.edu.fpt.model.VoucherType;
import vn.edu.fpt.util.Messages;
import vn.edu.fpt.util.Page;

public class VoucherService {
    public static final int PAGE_SIZE = 10;
    public static final String ACTIVE = "Active";
    public static final String USED = "Used";
    public static final String EXPIRED = "Expired";
    public static final String DISABLED = "Disabled";
    public static final String AMOUNT = "Amount";
    public static final String PERCENT = "Percent";
    private static final int MAX_CODE_LENGTH = 32;
    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z0-9-]{4,32}");
    private static final int SQL_UNIQUE_VIOLATION = 2627;
    private static final int SQL_UNIQUE_INDEX_VIOLATION = 2601;

    private final VoucherDAO voucherDAO = new VoucherDAO();

    public Page<Voucher> list(String keyword, String voucherType, int page) throws SQLException {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String normalizedType = VoucherType.ALL.contains(voucherType) ? voucherType : "";
        return Page.of(page, PAGE_SIZE, voucherDAO.count(normalizedKeyword, normalizedType),
                (offset, limit) -> voucherDAO.search(normalizedKeyword, normalizedType, offset, limit));
    }

    public Map<String, Integer> refundInventory() throws SQLException {
        return voucherDAO.countRefundInventory();
    }

    public void createPromotion(VoucherForm form, Integer createdByUserId) throws SQLException, ValidationException {
        Map<String, String> errors = validate(form);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        Voucher voucher = new Voucher();
        voucher.setVoucherCode(form.getVoucherCode().trim().toUpperCase());
        voucher.setDiscountType(form.getDiscountType());
        voucher.setDiscountValue(new BigDecimal(form.getDiscountValue().trim()));
        voucher.setMinOrderAmount(new BigDecimal(form.getMinOrderAmount().trim()));
        voucher.setExpiryDate(LocalDateTime.parse(form.getExpiryDate().trim()));
        voucher.setVoucherType(VoucherType.PROMOTION);
        voucher.setStatus(ACTIVE);
        voucher.setCreatedByUserId(createdByUserId);
        try {
            voucherDAO.insertPromotion(voucher);
        } catch (SQLException e) {
            if (e.getErrorCode() == SQL_UNIQUE_VIOLATION || e.getErrorCode() == SQL_UNIQUE_INDEX_VIOLATION) {
                throw new ValidationException(Map.of("voucherCode", Messages.VOUCHER_CODE_DUPLICATE));
            }
            throw e;
        }
    }

    private Map<String, String> validate(VoucherForm form) throws SQLException {
        Map<String, String> errors = new LinkedHashMap<>();
        String code = form.getVoucherCode() == null ? "" : form.getVoucherCode().trim().toUpperCase();
        if (code.isEmpty()) {
            errors.put("voucherCode", Messages.format(Messages.MSG02, "Mã voucher"));
        } else if (code.length() > MAX_CODE_LENGTH || !CODE_PATTERN.matcher(code).matches()) {
            errors.put("voucherCode", Messages.VOUCHER_CODE_INVALID);
        } else if (voucherDAO.existsByCode(code)) {
            errors.put("voucherCode", Messages.VOUCHER_CODE_DUPLICATE);
        }

        String discountType = form.getDiscountType() == null ? "" : form.getDiscountType().trim();
        if (!AMOUNT.equals(discountType) && !PERCENT.equals(discountType)) {
            errors.put("discountType", Messages.VOUCHER_DISCOUNT_TYPE_INVALID);
        }

        BigDecimal discountValue = parseDecimal(form.getDiscountValue(), "discountValue", errors);
        if (discountValue != null) {
            if (discountValue.signum() <= 0 || (PERCENT.equals(discountType)
                    && discountValue.compareTo(BigDecimal.valueOf(100)) > 0)) {
                errors.put("discountValue", Messages.VOUCHER_DISCOUNT_VALUE_INVALID);
            } else if (AMOUNT.equals(discountType) && discountValue.stripTrailingZeros().scale() > 0) {
                errors.put("discountValue", Messages.VOUCHER_DISCOUNT_VALUE_INVALID);
            }
        }

        BigDecimal minOrderAmount = parseDecimal(form.getMinOrderAmount(), "minOrderAmount", errors);
        if (minOrderAmount != null && (minOrderAmount.signum() < 0
                || minOrderAmount.stripTrailingZeros().scale() > 0)) {
            errors.put("minOrderAmount", Messages.VOUCHER_MIN_ORDER_INVALID);
        }

        LocalDateTime expiryDate = null;
        try {
            expiryDate = LocalDateTime.parse(form.getExpiryDate() == null ? "" : form.getExpiryDate().trim());
        } catch (DateTimeParseException e) {
            errors.put("expiryDate", Messages.VOUCHER_EXPIRY_INVALID);
        }
        if (expiryDate != null && !expiryDate.isAfter(LocalDateTime.now())) {
            errors.put("expiryDate", Messages.VOUCHER_EXPIRY_INVALID);
        }
        return errors;
    }

    private BigDecimal parseDecimal(String value, String field, Map<String, String> errors) {
        try {
            if (value == null || value.isBlank()) {
                throw new NumberFormatException();
            }
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            errors.put(field, Messages.VOUCHER_NUMBER_INVALID);
            return null;
        }
    }
}
