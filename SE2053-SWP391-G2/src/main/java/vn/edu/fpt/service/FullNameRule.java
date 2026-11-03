package vn.edu.fpt.service;

import java.text.Normalizer;
import java.util.regex.Pattern;
import vn.edu.fpt.util.Messages;

// Quy tắc Họ và tên dùng chung cho Đăng ký (UC-01) và Hồ sơ (UC-10) để hai nơi không bị lệch nhau
public final class FullNameRule {
    // SRS màn hình Register: Full Name tối đa 50 ký tự
    public static final int MAX_LENGTH = 50;
    // Chỉ gồm chữ cái (kể cả tiếng Việt có dấu), các từ cách nhau đúng một khoảng trắng
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L}\\p{M}]+( [\\p{L}\\p{M}]+)*$");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private FullNameRule() {
    }

    /**
     * Chuẩn hóa về Unicode NFC (bộ gõ có thể gửi dấu tách rời) và gộp khoảng trắng thừa.
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String composed = Normalizer.normalize(raw, Normalizer.Form.NFC).trim();
        return WHITESPACE.matcher(composed).replaceAll(" ");
    }

    /**
     * @param fullName giá trị đã qua {@link #normalize(String)}
     * @return thông điệp lỗi, hoặc null nếu hợp lệ
     */
    public static String validate(String fullName) {
        if (fullName.isEmpty()) {
            return Messages.format(Messages.MSG02, "Họ và tên");
        }
        if (fullName.length() > MAX_LENGTH) {
            return Messages.format(Messages.MSG08, MAX_LENGTH);
        }
        if (!NAME_PATTERN.matcher(fullName).matches()) {
            return Messages.FULLNAME_INVALID;
        }
        return null;
    }
}
