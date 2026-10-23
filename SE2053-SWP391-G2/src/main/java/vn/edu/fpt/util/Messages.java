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
            "Phim còn suất chiếu chưa kết thúc. Vui lòng hủy các suất chiếu trước khi ẩn hoặc chuyển sang Đã kết thúc.";
    // UC-04 View Movie List
    public static final String MOVIE_LIST_EMPTY = "Hiện tại chưa có phim nào trong danh mục này.";
    // UC-06 View Movie Details
    public static final String MOVIE_DETAIL_UNAVAILABLE = "Phim này không còn tồn tại hoặc đã ngừng chiếu.";
    // UC-08 View Showtimes
    public static final String SHOWTIME_NONE_FOR_DATE = "Không có suất chiếu nào cho ngày này. Vui lòng chọn ngày khác.";
    // UC-24 Manage Showtimes
    public static final String SHOWTIME_SAVED = "Suất chiếu đã được tạo thành công.";
    public static final String SHOWTIME_UPDATED = "Suất chiếu đã được cập nhật thành công.";
    public static final String SHOWTIME_NOT_FOUND = "Không tìm thấy suất chiếu.";
    public static final String SHOWTIME_CANCELLED = "Đã hủy suất chiếu thành công.";
    public static final String SHOWTIME_DELETED = "Đã xóa suất chiếu thành công.";
    public static final String SHOWTIME_MOVIE_INVALID = "Vui lòng chọn phim hợp lệ (phim đang chiếu hoặc sắp chiếu).";
    public static final String SHOWTIME_ROOM_INVALID = "Vui lòng chọn phòng chiếu đang hoạt động.";
    public static final String SHOWTIME_ROOM_INACTIVE = "Phòng chiếu đã dừng hoạt động, không thể tạo suất chiếu mới.";
    public static final String SHOWTIME_ROOM_MAINTENANCE = "Phòng chiếu đang bảo trì sự cố thiết bị, không thể tạo suất chiếu mới.";
    public static final String SHOWTIME_START_TIME_REQUIRED = "Vui lòng chọn thời gian bắt đầu chiếu.";
    public static final String SHOWTIME_START_TIME_PAST = "Thời gian bắt đầu suất chiếu phải ở tương lai.";
    public static final String SHOWTIME_PAST_START = SHOWTIME_START_TIME_PAST;
    public static final String SHOWTIME_PRICE_INVALID = "Giá vé cơ bản phải từ 10.000đ trở lên và là bội số của 1.000đ.";
    public static final String SHOWTIME_COLLISION =
            "Phòng {0} đã có suất chiếu \"{1}\" ({2} - {3}). Vui lòng chọn khung giờ khác hoặc phòng khác.";
    public static final String SHOWTIME_IMMUTABLE_BOOKED =
            "Suất chiếu đã có vé đặt hoặc ghế đang giữ chỗ, không thể đổi phim, phòng hoặc giờ chiếu.";
    public static final String SHOWTIME_HAS_BOOKINGS = SHOWTIME_IMMUTABLE_BOOKED;
    public static final String SHOWTIME_DELETE_HAS_BOOKINGS =
            "Suất chiếu đã phát sinh dữ liệu liên quan, không thể xóa khỏi hệ thống.";
    public static final String SHOWTIME_DELETE_HAS_REFERENCES =
            "Suất chiếu đã phát sinh dữ liệu (vé, đơn đặt vé hoặc phiên giữ chỗ), không thể xóa. Bạn chỉ có thể hủy suất chiếu (nếu chưa bán vé).";
    public static final String SHOWTIME_CANCEL_HAS_SOLD_TICKETS =
            "Suất chiếu đã có vé bán cho khách hàng. Không thể hủy suất chiếu này để bảo vệ quyền lợi của khách hàng đã mua vé.";
    public static final String SHOWTIME_CANCEL_ALREADY = "Suất chiếu này đã bị hủy trước đó.";
    public static final String SHOWTIME_FORMAT_MISMATCH =
            "Phim \"{0}\" chỉ hỗ trợ định dạng [{1}], không tương thích với phòng [{2}] ({3}).";
    public static final String SHOWTIME_BEFORE_RELEASE_DATE =
            "Thời gian chiếu ({0}) không được trước ngày khởi chiếu của phim ({1}).";
    public static final String SHOWTIME_BATCH_SAVED =
            "Đã tạo thành công {0} suất chiếu cho phim \"{1}\" tại phòng {2}.";
    public static final String SHOWTIME_BATCH_EMPTY =
            "Không có suất chiếu hợp lệ nào để tạo. Vui lòng kiểm tra lại khoảng ngày và khung giờ.";
    public static final String SHOWTIME_BATCH_DATE_INVALID =
            "Ngày kết thúc phải từ ngày bắt đầu trở đi và không quá 30 ngày.";
    public static final String SHOWTIME_BATCH_SLOTS_REQUIRED =
            "Vui lòng chọn ít nhất một khung giờ chiếu trong ngày.";
    public static final String SHOWTIME_BATCH_DAYS_REQUIRED =
            "Vui lòng chọn ít nhất một thứ trong tuần để lên lịch.";
    public static final String SHOWTIME_STATUS_INVALID = "Trạng thái suất chiếu không hợp lệ.";
    // UC-25 Manage Rooms & Seat Layout
    public static final String ROOM_SAVED = "Đã lưu phòng chiếu và sơ đồ ghế.";
    public static final String ROOM_NOT_FOUND = "Không tìm thấy phòng chiếu.";
    public static final String ROOM_NAME_DUPLICATE = "Tên phòng đã tồn tại trong hệ thống.";
    public static final String ROOM_FORMAT_INVALID = "Định dạng chiếu chỉ gồm 2D, 3D hoặc IMAX.";
    public static final String ROOM_SIZE_INVALID = "Số hàng phải từ 1 đến {0} và số cột từ 1 đến {1}.";
    public static final String ROOM_LAYOUT_INVALID =
            "Sơ đồ ghế không hợp lệ (sai kích thước, có ô không xác định hoặc ghế đôi không chiếm đủ 2 ô liền nhau). "
            + "Vui lòng tạo lại lưới ghế.";
    public static final String ROOM_LAYOUT_EMPTY = "Sơ đồ phải có ít nhất một ghế đang sử dụng được.";
    public static final String ROOM_LAYOUT_UNSUPPORTED =
            "Sơ đồ ghế hiện tại có vị trí ngoài phạm vi hỗ trợ (hàng A-Z, cột 1-{0}) hoặc có ghế chồng lên nhau "
            + "(ghế đôi kiểu cũ chiếm 1 ô - hãy chạy sql/UC25_sweetbox_two_cells.sql) nên không chỉnh sửa được tại đây.";
    public static final String ROOM_LAYOUT_CHANGED =
            "Sơ đồ ghế vừa phát sinh vé hoặc giữ chỗ mới trong lúc lưu. Vui lòng tải lại trang và thử lại.";
    public static final String ROOM_FORMAT_LOCKED =
            "Phòng đang có {0} suất chiếu sắp tới nên không thể đổi định dạng chiếu.";
    public static final String ROOM_SEAT_REMOVE_LOCKED =
            "Ghế {0} đã có lịch sử vé, giữ chỗ hoặc báo cáo sự cố nên không thể xóa khỏi sơ đồ. Hãy khóa ghế thay vì xóa.";
    public static final String ROOM_SEAT_DISABLE_LOCKED =
            "Ghế {0} đã bán vé cho suất chiếu sắp tới nên không thể khóa.";
    public static final String ROOM_SEAT_TYPE_LOCKED =
            "Ghế {0} đã bán vé cho suất chiếu sắp tới nên không thể đổi loại ghế.";
    public static final String ROOM_INFO_SAVED = "Đã cập nhật thông tin phòng chiếu.";
    public static final String ROOM_STATUS_INVALID = "Trạng thái phòng chỉ gồm Hoạt động hoặc Dừng hoạt động.";
    public static final String ROOM_STATUS_MAINTENANCE_LOCKED =
            "Phòng đang bảo trì do sự cố thiết bị. Trạng thái chỉ đổi được khi sự cố được xử lý ở mục Sự cố thiết bị.";
    public static final String ROOM_STATUS_INACTIVE_LOCKED =
            "Phòng đang có {0} suất chiếu sắp tới nên không thể dừng hoạt động. Hãy hủy hoặc dời các suất chiếu đó trước.";
    public static final String ROOM_STATUS_CHANGED =
            "Trạng thái phòng vừa được thay đổi (ví dụ phòng vừa bị khóa bảo trì). Vui lòng tải lại trang và thử lại.";
    public static final String ROOM_SEAT_REPORT_LOCKED =
            "Ghế {0} đang có báo cáo sự cố chưa xử lý xong nên phải giữ trạng thái khóa.";
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
    public static final String VOUCHER_UPDATED = "Mã khuyến mãi đã được cập nhật.";
    public static final String VOUCHER_DELETED = "Mã khuyến mãi đã được xóa.";
    public static final String VOUCHER_NOT_FOUND = "Không tìm thấy voucher.";
    public static final String VOUCHER_SYSTEM_MANAGED = "Voucher hoàn 80% do hệ thống quản lý, không thể sửa hoặc xóa.";
    public static final String VOUCHER_DELETE_IN_USE = "Voucher đã được gắn với đơn đặt vé nên không thể xóa.";
    public static final String VOUCHER_IMMUTABLE = "Voucher đã sử dụng hoặc bị vô hiệu hóa nên không thể chỉnh sửa.";
    public static final String VOUCHER_ID_INVALID = "Voucher không hợp lệ. Vui lòng tải lại danh sách.";
    public static final String VOUCHER_CODE_INVALID = "Mã chỉ gồm chữ in hoa, chữ số, dấu gạch ngang; dài 4-32 ký tự.";
    public static final String VOUCHER_CODE_DUPLICATE = "Mã voucher đã tồn tại trong hệ thống.";
    public static final String VOUCHER_DISCOUNT_TYPE_INVALID = "Loại giảm giá không hợp lệ.";
    public static final String VOUCHER_DISCOUNT_VALUE_INVALID =
            "Mức giảm phải lớn hơn 0; giảm theo phần trăm không vượt quá 100%, giảm tiền phải là số nguyên.";
    public static final String VOUCHER_MIN_ORDER_INVALID = "Giá trị đơn tối thiểu phải là số nguyên không âm.";
    public static final String VOUCHER_NUMBER_INVALID = "Giá trị phải là số hợp lệ.";
    public static final String VOUCHER_EXPIRY_INVALID = "Hạn sử dụng phải là thời điểm trong tương lai.";
    public static final String VOUCHER_CAMPAIGN_RANGE_INVALID = "Thời gian bắt đầu phải trước thời gian kết thúc.";
    public static final String VOUCHER_USAGE_LIMIT_INVALID = "Giới hạn lượt dùng phải là số nguyên dương.";
    public static final String VOUCHER_USAGE_LIMIT_BELOW_USAGE =
            "Giới hạn lượt dùng không được thấp hơn số lượt đã dùng hoặc đang giữ.";
    public static final String CUSTOMER_DATA_NOT_FOUND = "Không tìm thấy dữ liệu tài khoản.";
    public static final String BOOKING_HISTORY_EMPTY = "Bạn chưa có lịch sử đặt vé nào.";
    public static final String VOUCHER_WALLET_EMPTY = "Ví voucher của bạn hiện đang trống.";
    public static final String NOTIFICATION_EMPTY = "Không có thông báo mới.";
    public static final String LOYALTY_HISTORY_EMPTY = "Bạn chưa có giao dịch điểm nào.";
    public static final String NOTIFICATION_MARKED_READ = "Đã đánh dấu tất cả thông báo là đã đọc.";
    public static final String TICKET_CANCELLATION_NOT_ALLOWED =
            "Vé này không ở trạng thái hợp lệ để hủy.";
    public static final String TICKET_CANCELLATION_WINDOW =
            "Không thể hủy vé trong vòng 2 giờ trước giờ chiếu theo quy định.";
    public static final String TICKET_CANCELLATION_SUCCESS =
            "Đã hủy vé và cấp voucher hoàn 80% giá vé vào ví của bạn.";
    public static final String CUSTOMER_ACTION_FAILED = "Không thể hoàn tất thao tác. Vui lòng thử lại.";

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

    // UC-03 Forgot & Reset Password
    public static final String FORGOT_PASSWORD_EMAIL_SENT =
            "Đường link khôi phục mật khẩu đã được gửi tới email của bạn. Vui lòng kiểm tra hộp thư (hiệu lực trong 15 phút).";
    public static final String FORGOT_PASSWORD_EMAIL_NOT_FOUND = "Email này chưa được đăng ký trong hệ thống.";
    public static final String RESET_PASSWORD_SUCCESS = "Khôi phục mật khẩu thành công! Bạn có thể đăng nhập bằng mật khẩu mới.";
    public static final String RESET_PASSWORD_TOKEN_INVALID =
            "Đường link khôi phục mật khẩu không hợp lệ hoặc đã hết hạn. Vui lòng gửi lại yêu cầu mới.";
    public static final String RESET_PASSWORD_SAME_AS_OLD = "Mật khẩu mới không được trùng với mật khẩu hiện tại.";

    // UC-09 View Real-Time Seat Map
    public static final String SEAT_MAP_SHOWTIME_NOT_FOUND = "Suất chiếu không tồn tại hoặc không còn mở bán.";
    public static final String SEAT_MAP_SHOWTIME_STARTED =
            "Suất chiếu đã bắt đầu hoặc đã kết thúc. Vui lòng chọn suất chiếu khác.";
    public static final String SEAT_MAP_ROOM_MAINTENANCE =
            "Phòng chiếu đang tạm khóa để bảo dưỡng thiết bị. Vui lòng chọn suất chiếu khác.";
    public static final String SEAT_MAP_NO_SEATS = "Phòng chiếu chưa được cấu hình sơ đồ ghế.";
    public static final String SEAT_MAP_FULLY_BOOKED = "Suất chiếu này đã hết ghế trống.";

    // UC-10 Manage Profile
    public static final String PROFILE_UPDATED = "Cập nhật thông tin thành công!";
    public static final String PROFILE_PASSWORD_CHANGED = "Đổi mật khẩu thành công!";
    public static final String PROFILE_CURRENT_PASSWORD_WRONG = "Mật khẩu hiện tại không chính xác.";
    public static final String PROFILE_PASSWORD_SAME_AS_OLD = "Mật khẩu mới không được trùng với mật khẩu hiện tại.";
    public static final String PROFILE_NOT_FOUND = "Không tìm thấy tài khoản. Vui lòng đăng nhập lại.";
    public static final String PROFILE_SESSION_REVOKED =
            "Mật khẩu tài khoản đã được thay đổi. Vui lòng đăng nhập lại.";

    // UC-30 Manage Accounts (Admin)
    public static final String USER_ROLE_SELF_LOCKED =
            "Bạn không thể tự thay đổi vai trò quản trị của chính tài khoản đang đăng nhập.";
    public static final String USER_STATUS_SELF_LOCKED =
            "Bạn không thể tự khóa hoặc hủy kích hoạt tài khoản quản trị của chính mình.";
    public static final String USER_LAST_ADMIN_ROLE_LOCKED =
            "Không thể thay đổi vai trò của tài khoản này vì đây là Quản trị viên (Admin) hoạt động duy nhất trong hệ thống.";
    public static final String USER_LAST_ADMIN_STATUS_LOCKED =
            "Không thể khóa hoặc hủy kích hoạt tài khoản này vì đây là Quản trị viên (Admin) hoạt động duy nhất trong hệ thống.";
    public static final String USER_NOT_FOUND = "Không tìm thấy tài khoản người dùng.";
    public static final String USER_ROLE_INVALID = "Vai trò không hợp lệ. Chỉ chấp nhận Admin, Manager, Staff hoặc Customer.";
    public static final String USER_STATUS_INVALID = "Trạng thái không hợp lệ. Chỉ chấp nhận Active, Suspended hoặc Deactivated.";
    public static final String USER_CREATED = "Tạo tài khoản mới thành công!";
    public static final String USER_PROFILE_UPDATED = "Cập nhật thông tin tài khoản thành công!";
    public static final String USER_ROLE_UPDATED = "Phân quyền vai trò tài khoản thành công!";
    public static final String USER_STATUS_UPDATED = "Cập nhật trạng thái tài khoản thành công!";
    public static final String USER_PASSWORD_RESET = "Đặt lại mật khẩu cho tài khoản thành công!";

    // UC-13 Book Ticket Online (E1-E4 theo đúng câu chữ SRS)
    public static final String BOOKING_HOLD_EXPIRED = "Phiên đặt vé đã hết hạn. Vui lòng chọn lại ghế.";
    public static final String BOOKING_SEAT_TAKEN =
            "Ghế bạn chọn vừa có người đặt giữ chỗ. Vui lòng chọn ghế khác.";
    public static final String BOOKING_VOUCHER_INVALID =
            "Mã giảm giá không hợp lệ hoặc không đủ điều kiện áp dụng.";
    public static final String BOOKING_POINTS_INSUFFICIENT = "Số điểm tích lũy của bạn không đủ để quy đổi.";
    public static final String BOOKING_SEAT_REQUIRED = "Vui lòng chọn ít nhất một ghế.";
    public static final String BOOKING_SEAT_LIMIT = "Mỗi đơn đặt vé chỉ được chọn tối đa {0} ghế.";
    public static final String BOOKING_SEAT_INVALID = "Ghế đã chọn không thuộc phòng chiếu của suất này.";
    public static final String BOOKING_AGE_REQUIRED =
            "Phim phân loại {0}: bạn cần xác nhận đủ độ tuổi quy định trước khi tiếp tục đặt vé.";
    public static final String BOOKING_CUSTOMER_ONLY = "Chỉ tài khoản khách hàng mới có thể đặt vé online.";
    public static final String BOOKING_POINTS_INVALID = "Số điểm quy đổi phải là số nguyên không âm.";
    public static final String BOOKING_POINTS_EXCEED =
            "Điểm chỉ được trừ vào tiền vé; đơn này dùng được tối đa {0} điểm.";
    public static final String BOOKING_CONCESSION_QUANTITY_INVALID = "Số lượng {0} phải từ 0 đến {1}.";
    public static final String BOOKING_CONCESSION_UNAVAILABLE = "Món {0} hiện đã ngừng bán hoặc không đủ hàng.";
    public static final String BOOKING_SHOWTIME_UNAVAILABLE =
            "Suất chiếu không còn mở bán. Đơn đặt vé đã được hủy và ghế đã được trả lại.";
    public static final String BOOKING_UPDATED = "Đã cập nhật đơn hàng.";
    public static final String BOOKING_CANCELLED = "Đã hủy đơn đặt vé và trả lại ghế.";
    public static final String BOOKING_NOT_FOUND = "Không tìm thấy đơn đặt vé.";
    public static final String BOOKING_NOT_PENDING = "Đơn đặt vé không còn ở trạng thái chờ thanh toán.";
    public static final String BOOKING_PAID = "Thanh toán thành công! Vé điện tử đã được phát hành.";
    public static final String BOOKING_VNPAY_AMOUNT_INVALID =
            "Số tiền thanh toán không hợp lệ cho VNPay. Vui lòng liên hệ nhân viên hỗ trợ.";
    public static final String BOOKING_VNPAY_START_FAILED =
            "Không thể khởi tạo thanh toán VNPay. Vui lòng thử lại hoặc liên hệ nhân viên hỗ trợ.";
    public static final String BOOKING_VNPAY_RETURN_PENDING =
            "VNPay đã tiếp nhận yêu cầu. Đơn chỉ được xác nhận sau khi hệ thống nhận kết quả thanh toán hợp lệ.";
    public static final String BOOKING_FREE_CONFIRMED = "Đặt vé thành công! Vé điện tử đã được phát hành.";
    public static final String BOOKING_NOT_FREE = "Đơn này cần được thanh toán qua VNPay.";


    public static final String SYSTEM_ERROR = "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.";

    private Messages() {
    }

    public static String format(String pattern, Object... args) {
        return MessageFormat.format(pattern, args);
    }
}
