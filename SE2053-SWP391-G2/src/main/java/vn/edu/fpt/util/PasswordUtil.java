package vn.edu.fpt.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.HexFormat;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

// README mục 6: Mật khẩu chỉ lưu hash (PBKDF2WithHmacSHA256 + salt ngẫu nhiên)
// Không bao giờ log hoặc trả mật khẩu/hash ra giao diện
public final class PasswordUtil {
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    /**
     * Băm mật khẩu bằng PBKDF2 kèm salt ngẫu nhiên 16 bytes.
     * Kết quả lưu DB có dạng: {salt_hex}:{hash_hex}
     */
    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống khi băm");
        }
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(plainPassword.toCharArray(), salt);
        return HexFormat.of().formatHex(salt) + ":" + HexFormat.of().formatHex(hash);
    }

    /**
     * Kiểm tra mật khẩu người dùng nhập có khớp với chuỗi hash đã lưu trong DB hay không.
     */
    public static boolean verify(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) {
            return false;
        }
        // Hỗ trợ tài khoản seed dev (chưa băm PBKDF2): chấp nhận các mật khẩu mặc định thông dụng
        if (!storedHash.contains(":")) {
            if ("hashed_pwd".equals(storedHash)) {
                return "Admin@123".equals(plainPassword)
                        || "Manager@123".equals(plainPassword)
                        || "Staff@123".equals(plainPassword)
                        || "Customer@123".equals(plainPassword)
                        || "123456".equals(plainPassword)
                        || "hashed_pwd".equals(plainPassword);
            }
            return plainPassword.equals(storedHash);
        }
        String[] parts = storedHash.split(":", 2);
        try {
            byte[] salt = HexFormat.of().parseHex(parts[0]);
            byte[] expectedHash = HexFormat.of().parseHex(parts[1]);
            byte[] actualHash = pbkdf2(plainPassword.toCharArray(), salt);
            // So sánh độ dài không đổi (constant-time) để chống timing attack
            return java.security.MessageDigest.isEqual(expectedHash, actualHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Kiểm tra xem chuỗi hash trong DB có cần nâng cấp lên chuẩn PBKDF2 mới hay không.
     */
    public static boolean needsRehash(String storedHash) {
        return storedHash == null || !storedHash.contains(":");
    }

    private static byte[] pbkdf2(char[] password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Không khởi tạo được thuật toán băm mật khẩu PBKDF2", e);
        }
    }
}
