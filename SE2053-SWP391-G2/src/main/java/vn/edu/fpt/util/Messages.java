package vn.edu.fpt.util;

import java.text.MessageFormat;

// Thông điệp hệ thống tập trung (mã MSGxx theo SRS, mục 2. System Messages)
public final class Messages {
    public static final String MSG01 = "Không có kết quả tìm kiếm.";
    public static final String MSG02 = "Trường {0} là bắt buộc.";
    public static final String MSG03 = "Cập nhật {0} thành công.";
    public static final String MSG04 = "Thêm {0} thành công.";
    public static final String MSG08 = "Vượt quá độ dài tối đa {0} ký tự.";

    // UC-23 Manage Movies
    public static final String MOVIE_SAVED = "Phim lưu lại thành công!";
    public static final String MOVIE_ARCHIVED = "Phim đã được chuyển sang trạng thái Ẩn.";
    public static final String MOVIE_NOT_FOUND = "Không tìm thấy phim.";
    public static final String MOVIE_TITLE_DUPLICATE = "Tên phim đã tồn tại trong hệ thống.";
    public static final String MOVIE_DURATION_INVALID = "Thời lượng phải là số nguyên từ 1 đến {0} phút.";
    public static final String MOVIE_DURATION_LOCKED =
            "Phim đã có suất chiếu chưa kết thúc, không thể thay đổi thời lượng.";
    public static final String MOVIE_AGE_RATING_INVALID = "Phân loại tuổi chỉ gồm P, C13, C16 hoặc C18.";
    public static final String MOVIE_STATUS_INVALID = "Trạng thái không hợp lệ.";
    public static final String MOVIE_DATE_INVALID = "Ngày khởi chiếu không hợp lệ.";
    public static final String MOVIE_DATE_OUT_OF_RANGE = "Ngày khởi chiếu phải từ {0} đến {1}.";
    public static final String MOVIE_COMING_SOON_DATE_PASSED =
            "Phim Sắp chiếu phải có ngày khởi chiếu từ hôm nay trở đi.";
    public static final String MOVIE_NOT_RELEASED_YET =
            "Phim chưa tới ngày khởi chiếu, chỉ có thể ở trạng thái Sắp chiếu hoặc Đã ẩn.";
    public static final String MOVIE_FORMAT_INVALID = "Định dạng chiếu không hợp lệ.";
    public static final String MOVIE_GENRE_INVALID = "Thể loại không hợp lệ.";
    public static final String MOVIE_POSTER_URL_INVALID =
            "Đường dẫn poster phải là URL http/https hợp lệ (không dấu, không khoảng trắng).";
    public static final String MOVIE_POSTER_FILE_INVALID = "Poster chỉ chấp nhận ảnh JPG, PNG hoặc WEBP.";
    public static final String MOVIE_POSTER_FILE_TOO_LARGE = "Poster vượt quá dung lượng tối đa 5MB.";
    public static final String MOVIE_POSTER_SAVE_FAILED = "Không lưu được file poster. Vui lòng thử lại.";
    public static final String MOVIE_TRAILER_URL_INVALID =
            "Trailer phải là đường dẫn YouTube hợp lệ (https://www.youtube.com/watch?v=...).";
    public static final String MOVIE_HAS_ACTIVE_SHOWTIMES =
            "Phim có suất chiếu đang/sắp diễn ra đã có vé đặt, không thể ẩn hoặc chuyển sang Đã kết thúc.";
    // UC-04 View Movie List
    public static final String MOVIE_LIST_EMPTY = "Hiện tại chưa có phim nào trong danh mục này.";
    // UC-05 Search & Filter Movies
    public static final String MOVIE_SEARCH_NO_RESULT = "Không tìm thấy phim phù hợp với từ khóa/bộ lọc của bạn.";
    // UC-34 Manage Movie Genres
    public static final String GENRE_SAVED = "Thể loại lưu lại thành công!";
    public static final String GENRE_DELETED = "Đã xóa thể loại.";
    public static final String GENRE_NOT_FOUND = "Không tìm thấy thể loại.";
    public static final String GENRE_NAME_DUPLICATE = "Tên thể loại đã tồn tại trong hệ thống.";
    public static final String GENRE_IN_USE =
            "Thể loại đang được gán cho {0} phim, không thể xóa. Hãy gỡ thể loại khỏi các phim đó trước.";
    // UC-27 Manage Concession Items
    public static final String CONCESSION_HIDDEN = "Món đã được chuyển sang trạng thái Ẩn.";
    public static final String CONCESSION_NOT_FOUND = "Không tìm thấy món.";
    public static final String CONCESSION_NAME_DUPLICATE = "Tên món đã tồn tại trong hệ thống.";
    public static final String CONCESSION_PRICE_INVALID = "Giá bán phải là số nguyên không âm (VND).";
    public static final String CONCESSION_STOCK_INVALID = "Tồn kho phải là số nguyên không âm.";
    public static final String CONCESSION_IMAGE_URL_INVALID = "Đường dẫn ảnh phải là URL http/https hợp lệ.";
    public static final String CONCESSION_STATUS_INVALID = "Trạng thái không hợp lệ.";
    // UC-21 Report Equipment Issue
    public static final String REPORT_CATEGORY_INVALID = "Loại thiết bị không hợp lệ.";
    public static final String REPORT_ROOM_INVALID = "Phòng chiếu không tồn tại.";
    public static final String REPORT_SEAT_INVALID = "Ghế không thuộc phòng chiếu đã chọn.";
    public static final String REPORT_URGENCY_INVALID = "Mức khẩn cấp không hợp lệ.";
    public static final String REPORT_SEAT_LOCKED = "Ghế đã được khóa bảo trì.";
    public static final String REPORT_SEAT_HAS_TICKETS =
            "Ghế đã được khóa bảo trì và đang có {0} vé cho suất sắp chiếu, cần đổi ghế cho khách.";
    public static final String REPORT_SEAT_DUPLICATE = "Ghế {0} đã có báo cáo #SC-{1} chưa xử lý xong.";
    // UC-26 Process Equipment Reports
    public static final String REPORT_NOT_FOUND = "Không tìm thấy báo cáo sự cố.";
    public static final String REPORT_STATUS_INVALID = "Trạng thái không hợp lệ.";
    public static final String REPORT_STATUS_BACKWARD = "Không thể chuyển báo cáo về trạng thái trước đó.";
    public static final String REPORT_NO_CHANGE = "Chưa có thay đổi nào để lưu.";
    public static final String REPORT_CHANGED = "Báo cáo vừa được người khác cập nhật, vui lòng mở lại.";
    public static final String REPORT_ROOM_LOCKED = "Đã khóa {0} để sửa chữa.";
    public static final String REPORT_ROOM_LOCKED_SHOWTIMES =
            "Đã khóa {0} để sửa chữa; phòng còn {1} suất chiếu sắp tới cần xử lý.";
    public static final String REPORT_ROOM_UNLOCKED = "Đã mở khóa {0}.";
    public static final String REPORT_SEAT_REOPENED = "Ghế {0} đã được mở lại.";
    public static final String REPORT_LOG_ROOM_LOCKED = "Khóa phòng chiếu để sửa chữa.";
    public static final String REPORT_LOG_ROOM_UNLOCKED = "Mở khóa phòng chiếu.";
    public static final String REPORT_LOG_SEAT_REOPENED = "Mở lại ghế {0}.";
    // UC-28 Manage Vouchers & Promotions
    public static final String VOUCHER_SAVED = "Mã khuyến mãi đã được tạo.";
    public static final String VOUCHER_CODE_INVALID = "Mã chỉ gồm chữ in hoa, chữ số, dấu gạch ngang; dài 4-32 ký tự.";
    public static final String VOUCHER_CODE_DUPLICATE = "Mã voucher đã tồn tại trong hệ thống.";
    public static final String VOUCHER_DISCOUNT_TYPE_INVALID = "Loại giảm giá không hợp lệ.";
    public static final String VOUCHER_DISCOUNT_VALUE_INVALID =
            "Mức giảm phải lớn hơn 0; giảm theo phần trăm không vượt quá 100%, giảm tiền phải là số nguyên.";
    public static final String VOUCHER_MIN_ORDER_INVALID = "Giá trị đơn tối thiểu phải là số nguyên không âm.";
    public static final String VOUCHER_NUMBER_INVALID = "Giá trị phải là số hợp lệ.";
    public static final String VOUCHER_EXPIRY_INVALID = "Hạn sử dụng phải là thời điểm trong tương lai.";

    // UC-01 Register Account
    public static final String REGISTER_SUCCESS = "Đăng ký tài khoản thành công! Vui lòng đăng nhập.";
    public static final String REGISTER_OTP_SENT = "Email xác nhận kèm mã OTP đã được gửi tới email của bạn. Vui lòng kiểm tra hộp thư.";
    public static final String REGISTER_OTP_RESENT = "Mã xác thực mới đã được gửi lại tới email của bạn.";
    public static final String REGISTER_OTP_INVALID = "Mã xác thực không chính xác hoặc đã hết hạn.";
    public static final String REGISTER_OTP_EXPIRED = "Mã xác thực đã hết hạn. Vui lòng bấm Gửi lại mã.";
    public static final String REGISTER_EMAIL_DUPLICATE = "Email này đã được sử dụng. Vui lòng chọn email khác hoặc đăng nhập!";
    public static final String REGISTER_PHONE_DUPLICATE = "Số điện thoại này đã được sử dụng bởi một tài khoản khác.";
    public static final String REGISTER_PHONE_INVALID = "Số điện thoại không hợp lệ (gồm 10 chữ số bắt đầu bằng số 0).";
    public static final String REGISTER_EMAIL_INVALID = "Địa chỉ email không đúng định dạng.";
    public static final String REGISTER_PASSWORD_LENGTH = "Mật khẩu phải từ {0} đến {1} ký tự.";
    public static final String REGISTER_PASSWORD_WEAK = "Mật khẩu phải chứa ít nhất 1 chữ thường, 1 chữ hoa và 1 chữ số.";
    public static final String REGISTER_PASSWORD_MISMATCH = "Mật khẩu xác nhận không khớp.";
    public static final String REGISTER_TERMS_REQUIRED = "Bạn cần tích chọn đồng ý với Điều khoản và Chính sách để đăng ký.";
    public static final String REGISTER_SESSION_EXPIRED = "Phiên đăng ký đã hết hạn. Vui lòng thực hiện lại từ đầu.";

    // UC-02 Login & Logout
    public static final String LOGIN_REQUIRED_IDENTIFIER = "Vui lòng nhập Email hoặc Số điện thoại.";
    public static final String LOGIN_REQUIRED_PASSWORD = "Vui lòng nhập mật khẩu.";
    public static final String LOGIN_FAILED = "Email/Số điện thoại hoặc mật khẩu không chính xác.";
    public static final String LOGIN_ACCOUNT_INACTIVE = "Tài khoản của bạn đã bị khóa hoặc chưa được kích hoạt. Vui lòng liên hệ ban quản trị.";
    public static final String LOGIN_SUCCESS = "Đăng nhập thành công! Chào mừng {0}.";
    public static final String LOGOUT_SUCCESS = "Bạn đã đăng xuất an toàn khỏi hệ thống.";
    public static final String ACCESS_DENIED = "Bạn không có quyền truy cập vào khu vực này.";

    // UC-09 View Real-Time Seat Map
    public static final String SEAT_MAP_SHOWTIME_NOT_FOUND = "Suất chiếu không tồn tại hoặc không còn mở bán.";
    public static final String SEAT_MAP_SHOWTIME_STARTED =
            "Suất chiếu đã bắt đầu hoặc đã kết thúc. Vui lòng chọn suất chiếu khác.";
    public static final String SEAT_MAP_ROOM_MAINTENANCE =
            "Phòng chiếu đang tạm khóa để bảo dưỡng thiết bị. Vui lòng chọn suất chiếu khác.";
    public static final String SEAT_MAP_NO_SEATS = "Phòng chiếu chưa được cấu hình sơ đồ ghế.";
    public static final String SEAT_MAP_FULLY_BOOKED = "Suất chiếu này đã hết ghế trống.";

    public static final String SYSTEM_ERROR = "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.";

    private Messages() {
    }

    public static String format(String pattern, Object... args) {
        return MessageFormat.format(pattern, args);
    }
}
