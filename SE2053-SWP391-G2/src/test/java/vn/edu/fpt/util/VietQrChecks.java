package vn.edu.fpt.util;

import java.math.BigDecimal;

/** Chạy tay: java -cp target/classes;target/test-classes vn.edu.fpt.util.VietQrChecks */
public final class VietQrChecks {
    public static void main(String[] args) throws Exception {
        // Giá trị kiểm tra chuẩn của CRC-16/CCITT-FALSE
        check(VietQr.crc16("123456789") == 0x29B1, "crc16 check value");
        String p = VietQr.payload(new BigDecimal("160000"), "CINELZGS6E");
        check(p.startsWith("000201010212"), "dynamic QR header");
        check(p.contains("0006970422" + "01100969712567"), "MB bin + account");
        check(p.contains("5406160000"), "amount field");
        check(p.contains("62140810CINELZGS6E"), "note field");
        check(Integer.parseInt(p.substring(p.length() - 4), 16) == VietQr.crc16(p.substring(0, p.length() - 4)), "crc");
        check(VietQr.dataUri(new BigDecimal("160000"), "CINELZGS6E", 240).startsWith("data:image/png;base64,"), "png");
        System.out.println("VietQrChecks OK: " + p);
    }

    private static void check(boolean ok, String name) {
        if (!ok) {
            throw new AssertionError(name);
        }
    }
}
