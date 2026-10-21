package vn.edu.fpt.util;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

// UC-01: Gửi email chứa mã OTP xác thực đăng ký tài khoản thành viên
public final class EmailService {
    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());
    // Dùng thread pool để gửi email nền, không làm chậm request của người dùng
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private EmailService() {
    }

    /**
     * Gửi mã OTP xác thực email đăng ký tài khoản (chạy bất đồng bộ).
     *
     * @param recipientEmail Địa chỉ email người nhận
     * @param fullName       Họ tên người nhận
     * @param otpCode        Mã OTP 6 số
     * @param expiryMinutes  Thời gian hiệu lực tính bằng phút
     */
    public static void sendRegistrationOtpAsync(String recipientEmail, String fullName, String otpCode, int expiryMinutes) {
        EXECUTOR.submit(() -> sendRegistrationOtpSync(recipientEmail, fullName, otpCode, expiryMinutes));
    }

    /**
     * Gửi email đồng bộ (hoặc ghi log nếu ở chế độ dev chưa cấu hình SMTP).
     */
    public static boolean sendRegistrationOtpSync(String recipientEmail, String fullName, String otpCode, int expiryMinutes) {
        String host = AppConfig.get("mail.smtp.host", "smtp.gmail.com");
        String port = AppConfig.get("mail.smtp.port", "587");
        String senderEmail = AppConfig.get("mail.sender.email", "");
        String senderPassword = AppConfig.get("mail.sender.password", "").replace(" ", "");
        boolean auth = AppConfig.getBoolean("mail.smtp.auth", true);
        boolean starttls = AppConfig.getBoolean("mail.smtp.starttls.enable", true);

        // DEV MODE FALLBACK: Nếu chưa cấu hình email hoặc mật khẩu ứng dụng, log OTP ra console để test dễ dàng
        if (senderEmail.isEmpty() || senderPassword.isEmpty()) {
            LOGGER.info("""
                    ================================================================================
                    [DEV MODE - UC-01 EMAIL OTP]
                    Gửi tới: %s (%s)
                    Mã xác thực OTP: %s (Hết hạn sau %d phút)
                    (Để gửi mail thật: điền mail.sender.email và mail.sender.password trong config.properties)
                    ================================================================================
                    """.formatted(recipientEmail, fullName, otpCode, expiryMinutes));
            return true;
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", String.valueOf(auth));
        props.put("mail.smtp.starttls.enable", String.valueOf(starttls));
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail, "CineLuxe Cinema", StandardCharsets.UTF_8.name()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("[CineLuxe] Mã xác thực đăng ký tài khoản");

            String htmlContent = buildOtpEmailHtml(fullName, otpCode, expiryMinutes);
            message.setContent(htmlContent, "text/html; charset=UTF-8");

            Transport.send(message);
            LOGGER.info("Đã gửi thành công email OTP tới: " + recipientEmail);
            return true;
        } catch (MessagingException | UnsupportedEncodingException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi gửi email OTP tới: " + recipientEmail, e);
            return false;
        }
    }

    private static String buildOtpEmailHtml(String fullName, String otpCode, int expiryMinutes) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #0f131d; color: #dfe2f1; margin: 0; padding: 20px; }
                        .container { max-width: 520px; margin: 0 auto; background-color: #171b26; border-radius: 12px; border: 1px solid rgba(255,255,255,0.1); padding: 32px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
                        .logo { text-align: center; margin-bottom: 24px; }
                        .logo-text { font-size: 28px; font-weight: 800; color: #e50914; letter-spacing: 2px; }
                        .title { font-size: 20px; font-weight: 700; margin-bottom: 12px; color: #ffffff; text-align: center; }
                        .desc { font-size: 14px; line-height: 1.6; color: #e9bcb6; text-align: center; margin-bottom: 24px; }
                        .otp-box { background-color: #0f131d; border: 2px dashed #e50914; border-radius: 10px; padding: 18px; text-align: center; margin: 24px 0; }
                        .otp-code { font-size: 36px; font-weight: 800; letter-spacing: 8px; color: #ffb95f; }
                        .footer { font-size: 12px; color: #8e919f; text-align: center; margin-top: 30px; border-top: 1px solid rgba(255,255,255,0.08); padding-top: 16px; }
                        .warning { color: #ffb4ab; font-size: 13px; text-align: center; margin-top: 16px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="logo">
                            <span class="logo-text">CINELUXE</span>
                        </div>
                        <div class="title">Xác thực đăng ký tài khoản</div>
                        <p class="desc">Xin chào <strong>%s</strong>,<br>Cảm ơn bạn đã đăng ký tài khoản thành viên tại rạp phim CineLuxe. Dưới đây là mã xác thực OTP của bạn:</p>
                        <div class="otp-box">
                            <div class="otp-code">%s</div>
                        </div>
                        <p class="desc">Mã xác thực có hiệu lực trong vòng <strong>%d phút</strong>. Vui lòng không chia sẻ mã này cho bất kỳ ai để bảo vệ tài khoản của bạn.</p>
                        <p class="warning">Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email.</p>
                        <div class="footer">
                            &copy; CineLuxe Cinema - Trải nghiệm điện ảnh đỉnh cao. Mọi quyền được bảo lưu.
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(fullName, otpCode, expiryMinutes);
    }

    /**
     * UC-03: Gửi link khôi phục mật khẩu (chạy bất đồng bộ).
     */
    public static void sendPasswordResetLinkAsync(String recipientEmail, String fullName, String resetLink, int expiryMinutes) {
        EXECUTOR.submit(() -> sendPasswordResetLinkSync(recipientEmail, fullName, resetLink, expiryMinutes));
    }

    /**
     * UC-03: Gửi email link khôi phục mật khẩu đồng bộ (hoặc ghi log console ở chế độ dev).
     */
    public static boolean sendPasswordResetLinkSync(String recipientEmail, String fullName, String resetLink, int expiryMinutes) {
        String host = AppConfig.get("mail.smtp.host", "smtp.gmail.com");
        String port = AppConfig.get("mail.smtp.port", "587");
        String senderEmail = AppConfig.get("mail.sender.email", "");
        String senderPassword = AppConfig.get("mail.sender.password", "").replace(" ", "");
        boolean auth = AppConfig.getBoolean("mail.smtp.auth", true);
        boolean starttls = AppConfig.getBoolean("mail.smtp.starttls.enable", true);

        // DEV MODE FALLBACK: Ghi link ra console để test nhanh nếu chưa có cấu hình mail thật
        if (senderEmail.isEmpty() || senderPassword.isEmpty()) {
            LOGGER.info("""
                    ================================================================================
                    [DEV MODE - UC-03 PASSWORD RESET LINK]
                    Gửi tới: %s (%s)
                    Link đặt lại mật khẩu: %s
                    (Hiệu lực trong %d phút)
                    ================================================================================
                    """.formatted(recipientEmail, fullName, resetLink, expiryMinutes));
            return true;
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", String.valueOf(auth));
        props.put("mail.smtp.starttls.enable", String.valueOf(starttls));
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail, "CineLuxe Cinema", StandardCharsets.UTF_8.name()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("[CineLuxe] Hướng dẫn khôi phục mật khẩu tài khoản");

            String htmlContent = buildPasswordResetEmailHtml(fullName, resetLink, expiryMinutes);
            message.setContent(htmlContent, "text/html; charset=UTF-8");

            Transport.send(message);
            LOGGER.info("Đã gửi thành công email khôi phục mật khẩu tới: " + recipientEmail);
            return true;
        } catch (MessagingException | UnsupportedEncodingException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi gửi email khôi phục mật khẩu tới: " + recipientEmail, e);
            return false;
        }
    }

    private static String buildPasswordResetEmailHtml(String fullName, String resetLink, int expiryMinutes) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #0f131d; color: #dfe2f1; margin: 0; padding: 20px; }
                        .container { max-width: 520px; margin: 0 auto; background-color: #171b26; border-radius: 12px; border: 1px solid rgba(255,255,255,0.1); padding: 32px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
                        .logo { text-align: center; margin-bottom: 24px; }
                        .logo-text { font-size: 28px; font-weight: 800; color: #e50914; letter-spacing: 2px; }
                        .title { font-size: 20px; font-weight: 700; margin-bottom: 12px; color: #ffffff; text-align: center; }
                        .desc { font-size: 14px; line-height: 1.6; color: #dfe2f1; text-align: center; margin-bottom: 24px; }
                        .btn-box { text-align: center; margin: 28px 0; }
                        .btn { display: inline-block; background: linear-gradient(135deg, #e50914, #b80710); color: #ffffff !important; font-size: 15px; font-weight: 700; text-decoration: none; padding: 14px 32px; border-radius: 10px; box-shadow: 0 4px 15px rgba(229, 9, 20, 0.4); }
                        .link-box { background-color: #0f131d; border: 1px solid rgba(255,255,255,0.08); border-radius: 8px; padding: 12px; font-size: 12px; color: #ffb95f; word-break: break-all; margin: 16px 0; }
                        .footer { font-size: 12px; color: #8e919f; text-align: center; margin-top: 30px; border-top: 1px solid rgba(255,255,255,0.08); padding-top: 16px; }
                        .warning { color: #ffb4ab; font-size: 13px; text-align: center; margin-top: 16px; line-height: 1.5; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="logo">
                            <span class="logo-text">CINELUXE</span>
                        </div>
                        <div class="title">Khôi phục mật khẩu tài khoản</div>
                        <p class="desc">Xin chào <strong>%s</strong>,<br>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản CineLuxe của bạn. Vui lòng bấm vào nút bên dưới để tiến hành đổi mật khẩu mới:</p>
                        <div class="btn-box">
                            <a href="%s" target="_blank" class="btn">Khôi phục mật khẩu</a>
                        </div>
                        <p class="desc" style="font-size: 13px; color: #8e919f;">Liên kết này có hiệu lực trong vòng <strong>%d phút</strong> và chỉ sử dụng được 1 lần duy nhất.</p>
                        <p class="warning">Nếu nút bấm không hoạt động, bạn có thể sao chép liên kết dưới đây và dán vào thanh địa chỉ trình duyệt:</p>
                        <div class="link-box">%s</div>
                        <p class="warning">Nếu bạn không gửi yêu cầu này, xin vui lòng bỏ qua email. Mật khẩu của bạn vẫn được bảo mật an toàn.</p>
                        <div class="footer">
                            &copy; CineLuxe Cinema - Trải nghiệm điện ảnh đỉnh cao. Mọi quyền được bảo lưu.
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(fullName, resetLink, expiryMinutes, resetLink);
    }
}

