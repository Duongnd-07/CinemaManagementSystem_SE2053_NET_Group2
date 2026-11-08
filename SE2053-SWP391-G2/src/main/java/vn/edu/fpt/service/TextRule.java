package vn.edu.fpt.service;

// Kiểm tra chữ người dùng gõ (mô tả, lý do, ghi chú, tên): độ dài sau khi cắt khoảng trắng nằm trong [min, max]
// và có ít nhất một chữ cái, để chặn nội dung vô nghĩa như ".", "123", "!!!"
final class TextRule {
    private TextRule() {
    }

    static boolean ok(String value, int min, int max) {
        String text = value == null ? "" : value.trim();
        return text.length() >= min && text.length() <= max && text.codePoints().anyMatch(Character::isLetter);
    }
}
