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
    public static final String MOVIE_DURATION_INVALID = "Thời lượng phải là số nguyên lớn hơn 0.";
    public static final String MOVIE_AGE_RATING_INVALID = "Phân loại tuổi chỉ gồm P, C13, C16 hoặc C18.";
    public static final String MOVIE_STATUS_INVALID = "Trạng thái không hợp lệ.";
    public static final String MOVIE_DATE_INVALID = "Ngày khởi chiếu không hợp lệ.";
    public static final String MOVIE_FORMAT_INVALID = "Định dạng chiếu không hợp lệ.";
    public static final String MOVIE_GENRE_INVALID = "Thể loại không hợp lệ.";
    public static final String MOVIE_POSTER_URL_INVALID = "Đường dẫn poster phải là URL http/https hợp lệ.";
    public static final String MOVIE_POSTER_FILE_INVALID = "Poster chỉ chấp nhận ảnh JPG, PNG hoặc WEBP.";
    public static final String MOVIE_POSTER_FILE_TOO_LARGE = "Poster vượt quá dung lượng tối đa 5MB.";
    public static final String MOVIE_TRAILER_URL_INVALID = "Trailer phải là đường dẫn YouTube hợp lệ.";
    public static final String MOVIE_HAS_ACTIVE_SHOWTIMES =
            "Phim có suất chiếu đang/sắp diễn ra đã có vé đặt, không thể ẩn.";
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

    public static final String SYSTEM_ERROR = "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.";

    private Messages() {
    }

    public static String format(String pattern, Object... args) {
        return MessageFormat.format(pattern, args);
    }
}
