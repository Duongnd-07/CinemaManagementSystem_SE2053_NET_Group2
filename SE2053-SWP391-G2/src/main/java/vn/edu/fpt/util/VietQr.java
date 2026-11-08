package vn.edu.fpt.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import javax.imageio.ImageIO;

// UC-19: mã VietQR (chuẩn EMVCo của Napas 247) để khách quét chuyển khoản tại quầy; app ngân hàng tự điền sẵn
// tài khoản nhận, số tiền của đơn và nội dung chuyển khoản (mã đối chiếu CINE...)
public final class VietQr {
    public static final String BANK_NAME = "MB Bank";
    public static final String ACCOUNT_NAME = "DUONG HUU THAI SON";
    public static final String ACCOUNT_NO = "0969712567";
    // Mã BIN của MB Bank trong danh sách ngân hàng Napas
    private static final String BANK_BIN = "970422";
    private static final String NAPAS_GUID = "A000000727";
    private static final String TRANSFER_TO_ACCOUNT = "QRIBFTTA";

    private VietQr() {
    }

    // Nội dung mã: các trường ID + độ dài 2 chữ số + giá trị, kết thúc bằng CRC16 (trường 63)
    public static String payload(BigDecimal amount, String note) {
        String account = field("00", BANK_BIN) + field("01", ACCOUNT_NO);
        StringBuilder sb = new StringBuilder()
                .append(field("00", "01"))
                .append(field("01", "12")) // 12 = mã động, dùng cho một giao dịch
                .append(field("38", field("00", NAPAS_GUID) + field("01", account) + field("02", TRANSFER_TO_ACCOUNT)))
                .append(field("53", "704")) // VND
                .append(field("54", amount.setScale(0, RoundingMode.HALF_UP).toPlainString()))
                .append(field("58", "VN"));
        if (note != null && !note.isBlank()) {
            sb.append(field("62", field("08", note)));
        }
        sb.append("6304");
        return sb.append(String.format("%04X", crc16(sb.toString()))).toString();
    }

    // Ảnh PNG dạng data URI để nhúng thẳng vào trang
    public static String dataUri(BigDecimal amount, String note, int size) throws IOException {
        BitMatrix matrix;
        try {
            // Không đặt CHARACTER_SET: thêm khối ECI làm một số app ngân hàng không đọc được mã
            matrix = new QRCodeWriter().encode(payload(amount, note), BarcodeFormat.QR_CODE, size, size,
                    Map.of(EncodeHintType.MARGIN, 2));
        } catch (WriterException e) {
            throw new IOException("Không tạo được mã VietQR", e);
        }
        BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < matrix.getWidth(); x++) {
            for (int y = 0; y < matrix.getHeight(); y++) {
                image.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private static String field(String id, String value) {
        return id + String.format("%02d", value.length()) + value;
    }

    // CRC-16/CCITT-FALSE (đa thức 0x1021, khởi tạo 0xFFFF) theo chuẩn EMVCo
    static int crc16(String data) {
        int crc = 0xFFFF;
        for (byte b : data.getBytes(StandardCharsets.US_ASCII)) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
            }
        }
        return crc & 0xFFFF;
    }
}
