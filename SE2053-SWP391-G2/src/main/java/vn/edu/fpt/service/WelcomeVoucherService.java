package vn.edu.fpt.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.dao.CustomerNotificationDAO;
import vn.edu.fpt.dao.SystemConfigDAO;
import vn.edu.fpt.dao.VoucherDAO;
import vn.edu.fpt.model.Voucher;
import vn.edu.fpt.model.VoucherType;
import vn.edu.fpt.util.DBContext;

// UC-33: tặng voucher người mới khi khách đăng ký thành công (UC-01), giá trị/hạn/đơn tối thiểu do Admin cấu hình.
// Voucher cá nhân, dùng một lần, giảm số tiền cố định. Bảng Voucher chỉ có loại Promotion/Refund80 (UC-28)
// nên lưu là khuyến mãi cá nhân giống voucher đền bù ghế hỏng.
public class WelcomeVoucherService {
    private static final Logger LOGGER = Logger.getLogger(WelcomeVoucherService.class.getName());
    private static final String CONFIG_VALUE = "WELCOME_VOUCHER_VALUE";
    private static final String CONFIG_DAYS = "WELCOME_VOUCHER_DAYS";
    private static final String CONFIG_MIN_ORDER = "WELCOME_VOUCHER_MIN_ORDER";
    private static final BigDecimal DEFAULT_VALUE = BigDecimal.valueOf(50_000);
    private static final int DEFAULT_DAYS = 30;
    private static final String CODE_PREFIX = "WELCOME-";

    private final SystemConfigDAO configDAO = new SystemConfigDAO();
    private final VoucherDAO voucherDAO = new VoucherDAO();
    private final CustomerNotificationDAO notificationDAO = new CustomerNotificationDAO();

    // Lỗi khi tặng voucher không được làm hỏng việc đăng ký: chỉ ghi log và trả về false
    public boolean grant(int userId) {
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                BigDecimal value = configDAO.getDecimal(conn, CONFIG_VALUE, DEFAULT_VALUE);
                // Giá trị 0 = Admin đã tắt voucher người mới
                if (value.signum() <= 0) {
                    return false;
                }
                int days = Math.max(1, configDAO.getInt(conn, CONFIG_DAYS, DEFAULT_DAYS));
                BigDecimal minOrder = configDAO.getDecimal(conn, CONFIG_MIN_ORDER, BigDecimal.ZERO).max(BigDecimal.ZERO);

                Voucher voucher = new Voucher();
                voucher.setVoucherCode(CODE_PREFIX + UUID.randomUUID().toString().replace("-", "")
                        .substring(0, 12).toUpperCase(Locale.ROOT));
                voucher.setDiscountValue(value);
                voucher.setDiscountType(VoucherService.AMOUNT);
                voucher.setExpiryDate(LocalDateTime.now().plusDays(days));
                voucher.setOwnerUserId(userId);
                voucher.setStatus(VoucherService.ACTIVE);
                voucher.setVoucherType(VoucherType.PROMOTION);
                voucher.setMinOrderAmount(minOrder);
                voucherDAO.insertRefundVoucher(conn, voucher);
                notificationDAO.insert(conn, userId, "Voucher chào mừng thành viên mới",
                        "Cảm ơn bạn đã đăng ký. Voucher " + voucher.getVoucherCode() + " trị giá "
                                + value.stripTrailingZeros().toPlainString() + "đ đã được thêm vào ví, hạn dùng "
                                + days + " ngày.",
                        "Voucher");
                conn.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException | RuntimeException e) {
            LOGGER.log(Level.WARNING, "Không tặng được voucher người mới cho user " + userId, e);
            return false;
        }
    }
}
