package vn.edu.fpt.util;

import jakarta.activation.DataHandler;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.model.BookingConfirmation;
import vn.edu.fpt.model.ReminderTicket;
import vn.edu.fpt.model.ShowtimeReminder;

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

    public static boolean sendShowtimeReminderSync(ShowtimeReminder reminder) {
        String senderEmail = AppConfig.get("mail.sender.email", "");
        String senderPassword = AppConfig.get("mail.sender.password", "").replace(" ", "");
        if (senderEmail.isEmpty() || senderPassword.isEmpty()) {
            LOGGER.warning("JOB-02: SMTP chưa được cấu hình, không gửi email nhắc suất chiếu.");
            return false;
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", AppConfig.get("mail.smtp.host", "smtp.gmail.com"));
        props.put("mail.smtp.port", AppConfig.get("mail.smtp.port", "587"));
        props.put("mail.smtp.auth", String.valueOf(AppConfig.getBoolean("mail.smtp.auth", true)));
        props.put("mail.smtp.starttls.enable", String.valueOf(AppConfig.getBoolean("mail.smtp.starttls.enable", true)));
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
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(reminder.getRecipientEmail()));
            message.setSubject("[CineLuxe] Nhắc lịch xem phim");
            message.setContent(showtimeReminderHtml(reminder), "text/html; charset=UTF-8");
            Transport.send(message);
            return true;
        } catch (MessagingException | UnsupportedEncodingException e) {
            LOGGER.log(Level.WARNING, "JOB-02: không gửi được email nhắc suất chiếu", e);
            return false;
        }
    }

    private static String showtimeReminderHtml(ShowtimeReminder reminder) {
        StringBuilder tickets = new StringBuilder();
        for (ReminderTicket ticket : reminder.getTickets()) {
            tickets.append("<li>Ghế ").append(escapeHtml(ticket.getSeatLabel()))
                    .append(" · Mã vé ").append(escapeHtml(ticket.getTicketCode()))
                    .append(" · QR ").append(escapeHtml(ticket.getQrCode())).append("</li>");
        }
        String showtime = reminder.getShowtimeStart() == null ? ""
                : reminder.getShowtimeStart().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        return """
                <!DOCTYPE html>
                <html lang="vi"><body>
                    <h2>Xin chào %s,</h2>
                    <p>Đây là lời nhắc lịch xem phim <strong>%s</strong> vào lúc <strong>%s</strong>.</p>
                    <p>Vé của bạn:</p><ul>%s</ul>
                    <p>Vui lòng xuất trình mã QR trên vé tại rạp.</p>
                </body></html>
                """.formatted(escapeHtml(reminder.getRecipientName()), escapeHtml(reminder.getMovieTitle()),
                escapeHtml(showtime), tickets);
    }

    /**
     * UC-13/UC-14: gửi email xác nhận vé sau khi đơn online được thanh toán (chạy bất đồng bộ).
     */
    public static void sendBookingConfirmationAsync(BookingConfirmation confirmation) {
        EXECUTOR.submit(() -> sendBookingConfirmationSync(confirmation));
    }

    // Mã QR của từng vé nhúng thành ảnh inline (cid:) vì Gmail/Outlook chặn ảnh data: trong email
    public static boolean sendBookingConfirmationSync(BookingConfirmation confirmation) {
        String senderEmail = AppConfig.get("mail.sender.email", "");
        String senderPassword = AppConfig.get("mail.sender.password", "").replace(" ", "");
        if (senderEmail.isEmpty() || senderPassword.isEmpty()) {
            StringBuilder codes = new StringBuilder();
            for (ReminderTicket ticket : confirmation.getTickets()) {
                codes.append(ticket.getSeatLabel()).append('=').append(ticket.getTicketCode()).append(' ');
            }
            LOGGER.info("""
                    ================================================================================
                    [DEV MODE - UC-13 EMAIL XÁC NHẬN VÉ]
                    Gửi tới: %s (%s)
                    Đơn #%d - %s
                    Vé: %s
                    ================================================================================
                    """.formatted(confirmation.getRecipientEmail(), confirmation.getRecipientName(),
                    confirmation.getBookingId(), confirmation.getMovieTitle(), codes.toString().trim()));
            return true;
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", AppConfig.get("mail.smtp.host", "smtp.gmail.com"));
        props.put("mail.smtp.port", AppConfig.get("mail.smtp.port", "587"));
        props.put("mail.smtp.auth", String.valueOf(AppConfig.getBoolean("mail.smtp.auth", true)));
        props.put("mail.smtp.starttls.enable", String.valueOf(AppConfig.getBoolean("mail.smtp.starttls.enable", true)));
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        try {
            MimeMultipart content = new MimeMultipart("related");
            MimeBodyPart html = new MimeBodyPart();
            html.setContent(bookingConfirmationHtml(confirmation), "text/html; charset=UTF-8");
            content.addBodyPart(html);
            List<ReminderTicket> tickets = confirmation.getTickets();
            for (int i = 0; i < tickets.size(); i++) {
                if (tickets.get(i).getQrCode() == null) {
                    continue;
                }
                MimeBodyPart qr = new MimeBodyPart();
                qr.setDataHandler(new DataHandler(new ByteArrayDataSource(
                        QrCodeImage.png(ticketQrText(confirmation, tickets.get(i)), 440), "image/png")));
                qr.setContentID("<" + qrContentId(i) + ">");
                qr.setDisposition(MimeBodyPart.INLINE);
                qr.setFileName("QR-" + tickets.get(i).getTicketCode() + ".png");
                content.addBodyPart(qr);
            }

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail, "CineLuxe Cinema", StandardCharsets.UTF_8.name()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(confirmation.getRecipientEmail()));
            message.setSubject("[CineLuxe] Xác nhận đặt vé - Đơn #" + confirmation.getBookingId());
            message.setContent(content);
            Transport.send(message);
            LOGGER.info("Đã gửi email xác nhận vé đơn #" + confirmation.getBookingId());
            return true;
        } catch (MessagingException | IOException e) {
            LOGGER.log(Level.WARNING, "Không gửi được email xác nhận vé đơn #" + confirmation.getBookingId(), e);
            return false;
        }
    }

    private static String qrContentId(int index) {
        return "ticket-qr-" + index + "@cineluxe";
    }

    // Nội dung ảnh QR trong email: thông tin vé dạng văn bản để quét bằng camera điện thoại là đọc được như vé giấy
    // (không cần mạng, không phụ thuộc server). Ticket.QRCode trong DB không đổi, được ghi ở dòng "Mã soát vé".
    private static String ticketQrText(BookingConfirmation confirmation, ReminderTicket ticket) {
        String showtime = confirmation.getShowtimeStart() == null ? ""
                : confirmation.getShowtimeStart().format(DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy"));
        String room = nullToEmpty(confirmation.getRoomName())
                + (confirmation.getRoomFormat() == null ? "" : " (" + confirmation.getRoomFormat() + ")");
        return "CINELUXE - VÉ XEM PHIM\n"
                + "Phim: " + nullToEmpty(confirmation.getMovieTitle()) + "\n"
                + "Suất chiếu: " + showtime + "\n"
                + "Phòng: " + room + "\n"
                + "Ghế: " + nullToEmpty(ticket.getSeatLabel()) + "\n"
                + "Mã vé: " + nullToEmpty(ticket.getTicketCode()) + "\n"
                + "Mã đơn: #" + confirmation.getBookingId() + "\n"
                + "Mã soát vé: " + nullToEmpty(ticket.getQrCode());
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String bookingConfirmationHtml(BookingConfirmation confirmation) {
        StringBuilder tickets = new StringBuilder();
        List<ReminderTicket> lines = confirmation.getTickets();
        for (int i = 0; i < lines.size(); i++) {
            ReminderTicket ticket = lines.get(i);
            String qr = ticket.getQrCode() == null ? ""
                    : "<img src=\"cid:" + qrContentId(i) + "\" width=\"200\" height=\"200\" alt=\"QR vé "
                    + escapeHtml(ticket.getSeatLabel()) + "\" style=\"display:block;margin:8px auto;background:#fff;"
                    + "border-radius:8px;\"/><div style=\"font-size:11px;color:#8e919f;word-break:break-all;\">"
                    + escapeHtml(ticket.getQrCode()) + "</div>";
            tickets.append("<div class=\"ticket\"><div class=\"seat\">Ghế ").append(escapeHtml(ticket.getSeatLabel()))
                    .append("</div><div>Mã vé <strong>").append(escapeHtml(ticket.getTicketCode()))
                    .append("</strong></div>").append(qr).append("</div>");
        }
        String concessions = confirmation.getConcessions().isEmpty() ? ""
                : "<tr><td class=\"label\">Bắp nước</td><td>"
                + escapeHtml(String.join(", ", confirmation.getConcessions())) + "</td></tr>";
        String showtime = confirmation.getShowtimeStart() == null ? ""
                : confirmation.getShowtimeStart().format(DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy"));
        String room = escapeHtml(confirmation.getRoomName())
                + (confirmation.getRoomFormat() == null ? "" : " · " + escapeHtml(confirmation.getRoomFormat()));
        String amount = confirmation.getFinalAmount() == null ? "0"
                : NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(confirmation.getFinalAmount());
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #0f131d; color: #dfe2f1; margin: 0; padding: 20px; }
                        .container { max-width: 560px; margin: 0 auto; background-color: #171b26; border-radius: 12px; border: 1px solid rgba(255,255,255,0.1); padding: 32px; }
                        .logo-text { font-size: 28px; font-weight: 800; color: #e50914; letter-spacing: 2px; text-align: center; margin-bottom: 16px; }
                        .title { font-size: 20px; font-weight: 700; color: #ffffff; text-align: center; margin-bottom: 8px; }
                        .code { text-align: center; font-size: 15px; color: #ffb95f; margin-bottom: 20px; }
                        table { width: 100%%; border-collapse: collapse; font-size: 14px; margin-bottom: 20px; }
                        td { padding: 6px 0; vertical-align: top; color: #ffffff; }
                        td.label { color: #8e919f; width: 110px; }
                        .ticket { background-color: #0f131d; border: 1px dashed #e50914; border-radius: 10px; padding: 14px; margin-bottom: 12px; text-align: center; font-size: 14px; }
                        .seat { font-size: 18px; font-weight: 800; color: #ffb95f; margin-bottom: 4px; }
                        .note { font-size: 13px; color: #e9bcb6; text-align: center; line-height: 1.6; }
                        .footer { font-size: 12px; color: #8e919f; text-align: center; margin-top: 24px; border-top: 1px solid rgba(255,255,255,0.08); padding-top: 16px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="logo-text">CINELUXE</div>
                        <div class="title">Đặt vé thành công</div>
                        <div class="code">Mã đơn đặt vé: <strong>#%d</strong></div>
                        <p>Xin chào <strong>%s</strong>, cảm ơn bạn đã đặt vé tại CineLuxe.</p>
                        <table>
                            <tr><td class="label">Phim</td><td><strong>%s</strong></td></tr>
                            <tr><td class="label">Suất chiếu</td><td>%s</td></tr>
                            <tr><td class="label">Phòng</td><td>%s</td></tr>
                            %s
                            <tr><td class="label">Đã thanh toán</td><td>%sđ</td></tr>
                        </table>
                        %s
                        <p class="note">Vui lòng đưa mã QR hoặc mã vé cho nhân viên soát vé khi vào phòng chiếu.<br>Bạn cũng có thể xem lại vé trong mục Lịch sử đặt vé.</p>
                        <div class="footer">&copy; CineLuxe Cinema - Trải nghiệm điện ảnh đỉnh cao.</div>
                    </div>
                </body>
                </html>
                """.formatted(confirmation.getBookingId(), escapeHtml(confirmation.getRecipientName()),
                escapeHtml(confirmation.getMovieTitle()), escapeHtml(showtime), room, concessions, amount, tickets);
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
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

    /**
     * UC-10: Gửi mã OTP tới email mới để xác nhận đổi email đăng nhập (chạy bất đồng bộ).
     */
    public static void sendEmailChangeOtpAsync(String newEmail, String fullName, String otpCode, int expiryMinutes) {
        EXECUTOR.submit(() -> {
            String body = """
                    <p class="desc">Xin chào <strong>%s</strong>,<br>Bạn vừa yêu cầu đổi email đăng nhập tài khoản CineLuxe
                    sang địa chỉ này. Mã xác thực của bạn là:</p>
                    <div class="otp-box"><div class="otp-code">%s</div></div>
                    <p class="desc">Mã có hiệu lực trong <strong>%d phút</strong>. Không chia sẻ mã này cho bất kỳ ai.</p>
                    <p class="warning">Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email.</p>
                    """.formatted(escapeHtml(fullName), otpCode, expiryMinutes);
            sendSimpleHtml(newEmail, "[CineLuxe] Mã xác thực đổi email", "Xác thực email mới", body,
                    "UC-10 EMAIL CHANGE OTP: " + otpCode);
        });
    }

    /**
     * UC-10: Báo cho email cũ biết email đăng nhập vừa được đổi, để chủ tài khoản phát hiện nếu không phải họ đổi.
     */
    public static void sendEmailChangedNoticeAsync(String oldEmail, String fullName, String newEmail) {
        EXECUTOR.submit(() -> {
            String body = """
                    <p class="desc">Xin chào <strong>%s</strong>,<br>Email đăng nhập tài khoản CineLuxe của bạn vừa được
                    đổi sang <strong>%s</strong>. Từ nay vui lòng đăng nhập bằng email mới.</p>
                    <p class="warning">Nếu bạn không thực hiện thay đổi này, hãy liên hệ rạp ngay để được hỗ trợ.</p>
                    """.formatted(escapeHtml(fullName), escapeHtml(newEmail));
            sendSimpleHtml(oldEmail, "[CineLuxe] Email đăng nhập đã được thay đổi", "Email đăng nhập đã thay đổi",
                    body, "UC-10 EMAIL CHANGED NOTICE -> " + newEmail);
        });
    }

    private static void sendSimpleHtml(String recipient, String subject, String title, String bodyHtml,
                                       String devLogDetail) {
        String senderEmail = AppConfig.get("mail.sender.email", "");
        String senderPassword = AppConfig.get("mail.sender.password", "").replace(" ", "");
        // Chế độ dev chưa cấu hình SMTP: ghi ra log để vẫn test được luồng
        if (senderEmail.isEmpty() || senderPassword.isEmpty()) {
            LOGGER.info("[DEV MODE] Gửi tới: " + recipient + " | " + devLogDetail);
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", AppConfig.get("mail.smtp.host", "smtp.gmail.com"));
        props.put("mail.smtp.port", AppConfig.get("mail.smtp.port", "587"));
        props.put("mail.smtp.auth", String.valueOf(AppConfig.getBoolean("mail.smtp.auth", true)));
        props.put("mail.smtp.starttls.enable", String.valueOf(AppConfig.getBoolean("mail.smtp.starttls.enable", true)));
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #0f131d; color: #dfe2f1; margin: 0; padding: 20px; }
                        .container { max-width: 520px; margin: 0 auto; background-color: #171b26; border-radius: 12px; border: 1px solid rgba(255,255,255,0.1); padding: 32px; }
                        .logo { text-align: center; margin-bottom: 24px; font-size: 28px; font-weight: 800; color: #e50914; letter-spacing: 2px; }
                        .title { font-size: 20px; font-weight: 700; margin-bottom: 12px; color: #ffffff; text-align: center; }
                        .desc { font-size: 14px; line-height: 1.6; color: #e9bcb6; text-align: center; margin-bottom: 24px; }
                        .otp-box { background-color: #0f131d; border: 2px dashed #e50914; border-radius: 10px; padding: 18px; text-align: center; margin: 24px 0; }
                        .otp-code { font-size: 36px; font-weight: 800; letter-spacing: 8px; color: #ffb95f; }
                        .warning { color: #ffb4ab; font-size: 13px; text-align: center; margin-top: 16px; }
                        .footer { font-size: 12px; color: #8e919f; text-align: center; margin-top: 30px; border-top: 1px solid rgba(255,255,255,0.08); padding-top: 16px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="logo">CINELUXE</div>
                        <div class="title">%s</div>
                        %s
                        <div class="footer">&copy; CineLuxe Cinema - Trải nghiệm điện ảnh đỉnh cao.</div>
                    </div>
                </body>
                </html>
                """.formatted(title, bodyHtml);

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail, "CineLuxe Cinema", StandardCharsets.UTF_8.name()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            message.setSubject(subject);
            message.setContent(html, "text/html; charset=UTF-8");
            Transport.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi gửi email tới: " + recipient, e);
        }
    }
}
