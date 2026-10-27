package vn.edu.fpt.service;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import vn.edu.fpt.dao.MovieDAO;
import vn.edu.fpt.dao.VoucherDAO;
import vn.edu.fpt.model.Movie;
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
    public static final String SCOPE_ALL = "All";
    public static final String SCOPE_MOVIE = "Movie";
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999999999.99");
    private static final int MAX_CODE_LENGTH = 32;
    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z0-9-]{4,32}");
    private static final int SQL_UNIQUE_VIOLATION = 2627;
    private static final int SQL_UNIQUE_INDEX_VIOLATION = 2601;

    private final VoucherDAO voucherDAO = new VoucherDAO();
    private final MovieDAO movieDAO = new MovieDAO();

    public Page<Voucher> list(String keyword, String voucherType, int page) throws SQLException {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String normalizedType = VoucherType.ALL.contains(voucherType) ? voucherType : "";
        return Page.of(page, PAGE_SIZE, voucherDAO.count(normalizedKeyword, normalizedType),
                (offset, limit) -> voucherDAO.search(normalizedKeyword, normalizedType, offset, limit));
    }

    public Map<String, Integer> overview() throws SQLException {
        Map<String, Integer> overview = voucherDAO.countByTypeAndStatus();
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("PromotionActive", overview.getOrDefault(VoucherType.PROMOTION + ":Active", 0));
        result.put("PromotionExpired", overview.getOrDefault(VoucherType.PROMOTION + ":Expired", 0));
        result.put("Refund80Active", overview.getOrDefault(VoucherType.REFUND_80 + ":Active", 0));
        result.put("Refund80Expired", overview.getOrDefault(VoucherType.REFUND_80 + ":Expired", 0));
        return result;
    }

    public Voucher findById(int voucherId) throws SQLException {
        return voucherDAO.findById(voucherId);
    }

    public void save(Integer voucherId, VoucherForm form, Integer createdByUserId)
            throws SQLException, ValidationException {
        if (voucherId != null && voucherId <= 0) {
            throw new ValidationException(Map.of("general", Messages.VOUCHER_ID_INVALID));
        }
        Voucher existing = null;
        if (voucherId != null) {
            existing = voucherDAO.findById(voucherId);
            if (existing == null) {
                throw new ValidationException(Map.of("general", Messages.VOUCHER_NOT_FOUND));
            }
            if (!VoucherType.PROMOTION.equals(existing.getVoucherType())) {
                throw new ValidationException(Map.of("general", Messages.VOUCHER_SYSTEM_MANAGED));
            }
            if (USED.equals(existing.getStatus()) || DISABLED.equals(existing.getStatus())) {
                throw new ValidationException(Map.of("general", Messages.VOUCHER_IMMUTABLE));
            }
        }
        Map<String, String> errors = validate(form, voucherId);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        Voucher voucher = new Voucher();
        voucher.setVoucherId(voucherId == null ? 0 : voucherId);
        voucher.setVoucherCode(form.getVoucherCode().trim().toUpperCase());
        voucher.setDiscountType(form.getDiscountType());
        voucher.setDiscountValue(new BigDecimal(form.getDiscountValue().trim()));
        voucher.setMinOrderAmount(new BigDecimal(form.getMinOrderAmount().trim()));
        voucher.setStartDate(LocalDateTime.parse(form.getStartDate().trim()));
        voucher.setExpiryDate(LocalDateTime.parse(form.getExpiryDate().trim()));
        voucher.setUsageLimit(Integer.valueOf(form.getUsageLimit().trim()));
        voucher.setVoucherType(VoucherType.PROMOTION);
        voucher.setStatus(ACTIVE);
        voucher.setCreatedByUserId(createdByUserId);
        voucher.setMovieId(SCOPE_MOVIE.equals(form.getScope()) ? Integer.valueOf(form.getMovieId().trim()) : null);

        try {
            if (voucherId == null) {
                voucherDAO.insertPromotion(voucher);
            } else {
                voucher.setStatus(ACTIVE);
                voucher.setVoucherType(existing.getVoucherType());
                voucher.setOwnerUserId(existing.getOwnerUserId());
                voucher.setCreatedByUserId(existing.getCreatedByUserId());
                if (!voucherDAO.update(voucher)) {
                    if (voucherDAO.countActiveUses(voucherId) > voucher.getUsageLimit()) {
                        throw new ValidationException(Map.of(
                                "usageLimit", Messages.VOUCHER_USAGE_LIMIT_BELOW_USAGE));
                    }
                    throw new ValidationException(Map.of("general", Messages.VOUCHER_NOT_FOUND));
                }
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == SQL_UNIQUE_VIOLATION || e.getErrorCode() == SQL_UNIQUE_INDEX_VIOLATION) {
                throw new ValidationException(Map.of("voucherCode", Messages.VOUCHER_CODE_DUPLICATE));
            }
            throw e;
        }
    }

    public void delete(int voucherId) throws SQLException, ValidationException {
        Voucher voucher = voucherDAO.findById(voucherId);
        if (voucher == null) {
            throw new ValidationException(Map.of("general", Messages.VOUCHER_NOT_FOUND));
        }
        if (!VoucherType.PROMOTION.equals(voucher.getVoucherType())) {
            throw new ValidationException(Map.of("general", Messages.VOUCHER_SYSTEM_MANAGED));
        }
        try {
            if (!voucherDAO.deleteById(voucherId)) {
                throw new ValidationException(Map.of("general", Messages.VOUCHER_DELETE_IN_USE));
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 547) {
                throw new ValidationException(Map.of("general", Messages.VOUCHER_DELETE_IN_USE));
            }
            throw e;
        }
    }

    private Map<String, String> validate(VoucherForm form, Integer voucherId) throws SQLException {
        Map<String, String> errors = new LinkedHashMap<>();
        String code = form.getVoucherCode() == null ? "" : form.getVoucherCode().trim().toUpperCase();
        if (code.isEmpty()) {
            errors.put("voucherCode", Messages.format(Messages.MSG02, "Mã voucher"));
        } else if (code.length() > MAX_CODE_LENGTH || !CODE_PATTERN.matcher(code).matches()) {
            errors.put("voucherCode", Messages.VOUCHER_CODE_INVALID);
        } else if (voucherId == null || voucherId <= 0) {
            if (voucherDAO.existsByCode(code)) {
                errors.put("voucherCode", Messages.VOUCHER_CODE_DUPLICATE);
            }
        } else {
            Voucher existing = voucherDAO.findById(voucherId);
            if (existing != null && !existing.getVoucherCode().equalsIgnoreCase(code)
                    && voucherDAO.existsByCode(code)) {
                errors.put("voucherCode", Messages.VOUCHER_CODE_DUPLICATE);
            }
        }

        String discountType = form.getDiscountType() == null ? "" : form.getDiscountType().trim();
        if (!AMOUNT.equals(discountType) && !PERCENT.equals(discountType)) {
            errors.put("discountType", Messages.VOUCHER_DISCOUNT_TYPE_INVALID);
        }

        String scope = form.getScope() == null ? "" : form.getScope().trim();
        if (!SCOPE_ALL.equals(scope) && !SCOPE_MOVIE.equals(scope)) {
            errors.put("scope", Messages.VOUCHER_SCOPE_INVALID);
        } else if (SCOPE_MOVIE.equals(scope)) {
            try {
                int movieId = Integer.parseInt(form.getMovieId() == null ? "" : form.getMovieId().trim());
                Movie movie = movieId > 0 ? movieDAO.findById(movieId) : null;
                if (movie == null) {
                    errors.put("movieId", Messages.VOUCHER_MOVIE_INVALID);
                }
            } catch (NumberFormatException e) {
                errors.put("movieId", Messages.VOUCHER_MOVIE_INVALID);
            }
        }

        BigDecimal discountValue = parseDecimal(form.getDiscountValue(), "discountValue", errors);
        if (discountValue != null) {
            if (discountValue.signum() <= 0 || (PERCENT.equals(discountType)
                    && (discountValue.compareTo(BigDecimal.valueOf(100)) > 0
                    || discountValue.stripTrailingZeros().scale() > 2))
                    || discountValue.compareTo(MAX_AMOUNT) > 0) {
                errors.put("discountValue", Messages.VOUCHER_DISCOUNT_VALUE_INVALID);
            } else if (AMOUNT.equals(discountType) && discountValue.stripTrailingZeros().scale() > 0) {
                errors.put("discountValue", Messages.VOUCHER_DISCOUNT_VALUE_INVALID);
            }
        }

        BigDecimal minOrderAmount = parseDecimal(form.getMinOrderAmount(), "minOrderAmount", errors);
        if (minOrderAmount != null && (minOrderAmount.signum() < 0
                || minOrderAmount.stripTrailingZeros().scale() > 0
                || minOrderAmount.compareTo(MAX_AMOUNT) > 0)) {
            errors.put("minOrderAmount", Messages.VOUCHER_MIN_ORDER_INVALID);
        }

        LocalDateTime startDate = parseDateTime(form.getStartDate(), "startDate", errors);
        LocalDateTime expiryDate = null;
        if (form.getExpiryDate() != null && !form.getExpiryDate().isBlank()) {
            expiryDate = parseDateTime(form.getExpiryDate(), "expiryDate", errors);
        } else {
            errors.put("expiryDate", Messages.VOUCHER_EXPIRY_INVALID);
        }
        if (expiryDate != null && !expiryDate.isAfter(LocalDateTime.now())) {
            errors.put("expiryDate", Messages.VOUCHER_EXPIRY_INVALID);
        }
        if (startDate != null && expiryDate != null && !expiryDate.isAfter(startDate)) {
            errors.put("expiryDate", Messages.VOUCHER_CAMPAIGN_RANGE_INVALID);
        }

        Integer usageLimit = null;
        try {
            usageLimit = Integer.valueOf(form.getUsageLimit() == null ? "" : form.getUsageLimit().trim());
            if (usageLimit < 1) {
                errors.put("usageLimit", Messages.VOUCHER_USAGE_LIMIT_INVALID);
            } else if (voucherId != null && voucherDAO.countActiveUses(voucherId) > usageLimit) {
                errors.put("usageLimit", Messages.VOUCHER_USAGE_LIMIT_BELOW_USAGE);
            }
        } catch (NumberFormatException e) {
            errors.put("usageLimit", Messages.VOUCHER_USAGE_LIMIT_INVALID);
        }
        return errors;
    }

    private LocalDateTime parseDateTime(String value, String field, Map<String, String> errors) {
        try {
            return LocalDateTime.parse(value == null ? "" : value.trim());
        } catch (DateTimeParseException e) {
            errors.put(field, Messages.VOUCHER_CAMPAIGN_RANGE_INVALID);
            return null;
        }
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
