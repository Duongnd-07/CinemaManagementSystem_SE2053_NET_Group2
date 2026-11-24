package vn.edu.fpt.service;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import vn.edu.fpt.dao.TicketPrintDAO;
import vn.edu.fpt.model.BookingStatus;
import vn.edu.fpt.model.CheckInResult;
import vn.edu.fpt.model.DoorShowtime;
import vn.edu.fpt.model.PrintBooking;
import vn.edu.fpt.model.PrintTicket;
import vn.edu.fpt.model.RoomStatus;
import vn.edu.fpt.model.Shift;
import vn.edu.fpt.model.ShowtimeStatus;
import vn.edu.fpt.model.TicketStatus;
import vn.edu.fpt.util.DBContext;
import vn.edu.fpt.util.Messages;
import vn.edu.fpt.util.QrCodeImage;

// UC-36: Scan E-ticket & Print Paper Ticket. Nhân viên phải đang mở ca mới được quét / in vé (BR-36-07);
// soát vé tại cửa phòng chiếu (A7) không cần mở ca.
// A7: soát vé tại cửa phòng chiếu, mỗi vé chỉ vào cửa một lần và chỉ bản in mới nhất (BR-36-08..10).
// In vé và soát cửa không đổi trạng thái vé, đơn hay doanh thu ca.
// So giờ bằng đồng hồ của web server, cùng giả định với ShiftService (web server và SQL Server cùng giờ).
public class TicketPrintService {
    public static final String SHIFT_REQUIRED = "Bạn cần mở ca làm việc trước khi quét và in vé.";
    public static final String NOT_FOUND = "Không tìm thấy vé. Vui lòng kiểm tra lại mã vé.";
    public static final String NOT_PAID = "Đơn hàng chưa được thanh toán hoặc đã bị huỷ, không thể in vé.";
    public static final String CANCELLED = "Vé đã bị huỷ.";
    public static final String EXCHANGED = "Vé đã đổi sang ghế {0} (mã vé {1}).";
    public static final String EXCHANGED_UNKNOWN = "Vé đã đổi sang ghế khác.";
    public static final String TOO_EARLY = "Vé chỉ được in từ {0}.";
    public static final String ENDED = "Suất chiếu đã kết thúc hoặc đã bị huỷ, không thể in vé.";
    public static final String ALREADY_PRINTED = "Vé {0} đã được in lúc {1} bởi {2}.";
    public static final String BROKEN_SEAT =
            "Ghế {0} đang hỏng, không thể in vé. Vui lòng đổi ghế hoặc hoàn tiền cho khách trước.";
    public static final String COUNTER_PRINTED ="Vé bán tại quầy đã được in khi bán.";
    public static final String NOT_PRINTED_YET = "Vé chưa được in lần nào, hãy dùng nút In vé.";
    public static final String REASON_REQUIRED = "Vui lòng nhập lý do in lại (ít nhất 5 ký tự).";
    public static final String NOTHING_SELECTED = "Vui lòng chọn ít nhất một vé để in.";
    public static final String PRINTED = "Đã in {0} vé.";
    public static final String REPRINTED = "Đã in lại vé {0}.";
    public static final String PRINT_CANCELLED = "Đã hủy lượt in, vé không được tính là đã in.";
    public static final String PRINT_EXPIRED = "Lượt in này không còn hiệu lực. Vui lòng tra cứu lại vé và in lại.";
    // A7 - soát vé tại cửa phòng chiếu (E10-E14)
    public static final String CHECKED_IN_NO_PRINT = "Vé {0} đã vào cửa lúc {1}, không thể in hoặc in lại.";
    public static final String DOOR_USED = "Vé {0} đã vào cửa lúc {1} bởi {2}. Không cho vào lần nữa.";
    public static final String DOOR_OLD_PRINT =
            "Vé giấy này đã được thay bằng bản in lại lần {0}. Chỉ bản in mới nhất được vào cửa.";
    public static final String DOOR_BAD_PRINT = "Mã vé giấy không hợp lệ (chưa có lần in {0}).";
    public static final String DOOR_WRONG_SHOWTIME = "Vé thuộc suất {0} - {1}, không phải suất đang soát.";
    public static final String DOOR_TOO_EARLY = "Chưa đến giờ vào cửa (mở từ {0}).";
    public static final String DOOR_NO_SHOWTIME = "Vui lòng chọn suất chiếu đang soát.";
    public static final String DOOR_ADMITTED = "Mời vào - Ghế {0}, {1}";

    // Mở in từ 24 giờ trước giờ chiếu (trùng lúc hết hạn hủy vé online UC-16) đến khi suất chiếu kết thúc
    public static final int PRINT_WINDOW_HOURS = 24;
    // BR-36-09: mở cửa soát vé từ 30 phút trước giờ chiếu đến khi suất chiếu kết thúc
    public static final int DOOR_WINDOW_MINUTES = 30;
    private static final int QR_SIZE = 220;
    public static final int MIN_REASON = 5;
    public static final int MAX_REASON = 200;
    private static final int SQL_UNIQUE_VIOLATION = 2627;
    private static final int SQL_UNIQUE_INDEX_VIOLATION = 2601;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    // Ảnh QR trong email chứa cả đoạn thông tin vé (EmailService.ticketQrText); máy quét gõ dấu tiếng Việt
    // có thể sai nên tìm thẳng mã soát vé (32 ký tự hex) hoặc mã vé (TK + 10 ký tự) trong nội dung quét được
    private static final Pattern CHECK_CODE = Pattern.compile("\\b[0-9A-F]{32}\\b");
    private static final Pattern TICKET_CODE = Pattern.compile("\\bTK[A-Z0-9]{10}\\b");
    // QR soát cửa trên vé giấy: mã vé + "-P" + số lần in (BR-36-05, BR-36-10)
    private static final Pattern PAPER_CODE = Pattern.compile("\\bTK[A-Z0-9]{10}-P(\\d{1,3})\\b");

    private final TicketPrintDAO printDAO = new TicketPrintDAO();
    private final ShiftService shiftService = new ShiftService();

    public Shift requireShift(int staffId) throws SQLException, ValidationException {
        Shift shift = shiftService.findOpenShift(staffId);
        if (shift == null) {
            throw error(SHIFT_REQUIRED);
        }
        return shift;
    }

    // BR-36-01: mã vé hoặc mã soát vé, lấy từ nội dung QR nếu quét cả đoạn văn bản
    public String extractCode(String raw) {
        String text = raw == null ? "" : raw.toUpperCase(Locale.ROOT);
        Matcher check = CHECK_CODE.matcher(text);
        if (check.find()) {
            return check.group();
        }
        Matcher ticket = TICKET_CODE.matcher(text);
        if (ticket.find()) {
            return ticket.group();
        }
        return text.replaceAll("\\s+", "");
    }

    // Bước 3-4: tìm đơn theo mã của một vé bất kỳ trong đơn và tính vé nào in được
    public PrintBooking lookup(String raw) throws SQLException, ValidationException {
        String code = extractCode(raw);
        int bookingId = code.isEmpty() ? 0 : printDAO.findBookingIdByCode(code);
        return loadBooking(bookingId);
    }

    public PrintBooking loadBooking(int bookingId) throws SQLException, ValidationException {
        PrintBooking booking = bookingId > 0 ? printDAO.findBooking(bookingId) : null;
        if (booking == null) {
            throw error(NOT_FOUND);
        }
        evaluate(booking, LocalDateTime.now());
        return booking;
    }

    // Bước 6: chuẩn bị lượt in (các vé giấy + phiếu nhận bắp nước nếu đơn chưa in lần nào). CHƯA ghi DB: trình
    // duyệt không cho biết nhân viên bấm In hay Hủy trong hộp thoại in, nên chỉ ghi khi nhân viên xác nhận vé đã
    // ra giấy (confirm). Hủy, bấm Back hay rời trang đều không làm vé bị tính là đã in.
    public PrintBooking preparePrint(int bookingId, List<Integer> ticketIds, int staffId, String staffName)
            throws SQLException, ValidationException {
        requireShift(staffId);
        PrintBooking booking = loadBooking(bookingId);
        if (booking.getBlockReason() != null) {
            throw error(booking.getBlockReason());
        }
        Set<Integer> selected = Set.copyOf(ticketIds);
        List<PrintTicket> chosen = new ArrayList<>();
        for (PrintTicket ticket : booking.getTickets()) {
            if (!selected.contains(ticket.getTicketId())) {
                continue;
            }
            if (!ticket.isPrintable()) {
                throw error(ticket.getBlockReason());
            }
            chosen.add(ticket);
        }
        if (chosen.isEmpty()) {
            throw error(NOTHING_SELECTED);
        }
        LocalDateTime now = LocalDateTime.now();
        for (PrintTicket ticket : chosen) {
            ticket.setPrintCount(1);
            ticket.setPrintedAt(now);
            ticket.setPrintedBy(staffName);
            ticket.setPrintReason(null);
            ticket.setCheckInQr(qrDataUri(ticket.getPaperCode()));
        }
        booking.setConcessionSlip(!booking.getConcessions().isEmpty() && !printDAO.hasAnyPrint(bookingId));
        booking.setTickets(chosen);
        return booking;
    }

    // A2: chuẩn bị in lại vé giấy bị mất, bắt buộc lý do; lượt in chỉ gồm vé đó. Cũng chỉ ghi DB khi xác nhận.
    public PrintBooking prepareReprint(int ticketId, String reason, int staffId, String staffName)
            throws SQLException, ValidationException {
        requireShift(staffId);
        String cleanReason = reason == null ? "" : reason.trim();
        if (cleanReason.length() < MIN_REASON) {
            throw error(REASON_REQUIRED);
        }
        if (cleanReason.length() > MAX_REASON) {
            throw error(Messages.format(Messages.MSG08, String.valueOf(MAX_REASON)));
        }
        PrintBooking booking = loadBooking(printDAO.findBookingIdByTicket(ticketId));
        if (booking.getBlockReason() != null) {
            throw error(booking.getBlockReason());
        }
        PrintTicket ticket = booking.getTickets().stream().filter(t -> t.getTicketId() == ticketId).findFirst()
                .orElseThrow(() -> error(NOT_FOUND));
        if (!ticket.isReprintable()) {
            throw error(ticket.isPrintable() ? NOT_PRINTED_YET : ticket.getBlockReason());
        }
        ticket.setPrintCount(lastPrintNo(booking, ticket.getPrintCount()) + 1);
        ticket.setPrintedAt(LocalDateTime.now());
        ticket.setPrintedBy(staffName);
        ticket.setPrintReason(cleanReason);
        // Vé in lại mang số lần in mới: vé giấy cũ và vé điện tử không còn vào cửa được (BR-36-10)
        ticket.setCheckInQr(qrDataUri(ticket.getPaperCode()));
        booking.setTickets(List.of(ticket));
        booking.setConcessionSlip(false);
        return booking;
    }

    // Nhân viên xác nhận vé đã ra giấy: ghi lượt in vào DB. Kiểm tra lại vì giữa lúc mở hộp thoại in và lúc xác
    // nhận, vé có thể đã bị hủy / đổi ghế hoặc nhân viên khác đã in (E5). Ghi cả lượt hoặc không ghi gì.
    public void confirmPrint(PrintBooking job, int staffId, String staffName) throws SQLException, ValidationException {
        if (job == null || job.getTickets().isEmpty()) {
            throw error(PRINT_EXPIRED);
        }
        Shift shift = requireShift(staffId);
        PrintBooking current = loadBooking(job.getBookingId());
        if (current.getBlockReason() != null) {
            throw error(current.getBlockReason());
        }
        for (PrintTicket ticket : job.getTickets()) {
            PrintTicket now = current.getTickets().stream().filter(t -> t.getTicketId() == ticket.getTicketId())
                    .findFirst().orElseThrow(() -> error(NOT_FOUND));
            boolean stillValid = ticket.getPrintCount() == 1 ? now.isPrintable()
                    : now.isReprintable() && lastPrintNo(current, now.getPrintCount()) == ticket.getPrintCount() - 1;
            if (!stillValid) {
                throw error(now.getBlockReason() != null ? now.getBlockReason() : PRINT_EXPIRED);
            }
        }
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (PrintTicket ticket : job.getTickets()) {
                    int last = lastPrintNo(current, printDAO.lockLastPrintNo(conn, ticket.getTicketId()));
                    if (last != ticket.getPrintCount() - 1) {
                        conn.rollback();
                        throw alreadyPrinted(job.getBookingId(), ticket.getTicketId());
                    }
                    ticket.setPrintedAt(printDAO.insertPrint(conn, ticket.getTicketId(), ticket.getPrintCount(),
                            ticket.getPrintReason(), staffId, shift.getShiftId()));
                    ticket.setPrintedBy(staffName);
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                if (isDuplicate(e)) {
                    // E5: nhân viên khác vừa in cùng vé
                    throw alreadyPrinted(job.getBookingId(), job.getTickets().get(0).getTicketId());
                }
                throw e;
            }
        }
    }

    // Số lần in đã có; vé bán tại quầy coi như đã in lần 1 lúc bán (BR-36-06)
    private int lastPrintNo(PrintBooking booking, int printedInDb) {
        return Math.max(printedInDb, booking.isCounterSale() ? 1 : 0);
    }

    public String cancelledMessage() {
        return PRINT_CANCELLED;
    }

    public String printedMessage(int count) {
        return Messages.format(PRINTED, String.valueOf(count));
    }

    public String reprintedMessage(String ticketCode) {
        return Messages.format(REPRINTED, ticketCode);
    }

    // BR-36-02: đơn đã thanh toán, suất chưa hủy, trong khung in; từng vé Upcoming và chưa in
    private void evaluate(PrintBooking booking, LocalDateTime now) {
        String bookingBlock = null;
        if (!BookingStatus.PAID.equals(booking.getBookingStatus())) {
            bookingBlock = NOT_PAID;
        } else if (ShowtimeStatus.CANCELLED.equals(booking.getShowtimeStatus())
                || ShowtimeStatus.ENDED.equals(booking.getShowtimeStatus())
                || !now.isBefore(booking.getEndTime())) {
            bookingBlock = ENDED;
        } else if (now.isBefore(booking.getStartTime().minusHours(PRINT_WINDOW_HOURS))) {
            bookingBlock = Messages.format(TOO_EARLY, booking.getStartTime().minusHours(PRINT_WINDOW_HOURS).format(TIME));
        }
        booking.setBlockReason(bookingBlock);
        for (PrintTicket ticket : booking.getTickets()) {
            ticket.setPrintable(false);
            ticket.setReprintable(false);
            ticket.setBrokenSeat(false);
            if (TicketStatus.CANCELLED.equals(ticket.getStatus())) {
                ticket.setBlockReason(CANCELLED);
            } else if (TicketStatus.EXCHANGED.equals(ticket.getStatus())) {
                ticket.setBlockReason(ticket.getExchangedCode() == null ? EXCHANGED_UNKNOWN
                        : Messages.format(EXCHANGED, ticket.getExchangedSeat(), ticket.getExchangedCode()));
            } else if (!TicketStatus.UPCOMING.equals(ticket.getStatus())) {
                ticket.setBlockReason(ENDED);
            } else if (ticket.isCheckedIn()) {
                // E14: vé đã vào cửa thì không in / in lại nữa (BR-36-10)
                ticket.setBlockReason(Messages.format(CHECKED_IN_NO_PRINT, ticket.getTicketCode(),
                        ticket.getCheckedInAt().format(TIME)));
            } else if (RoomStatus.MAINTENANCE.equals(ticket.getSeatStatus())) {
                // E9: ghế đã báo hỏng sau khi bán vé - không in / in lại, chuyển sang đổi ghế hoặc hoàn tiền (UC-20)
                ticket.setBrokenSeat(true);
                ticket.setBlockReason(Messages.format(BROKEN_SEAT, ticket.getSeatLabel()));
            } else if (ticket.getPrintCount() > 0) {
                ticket.setBlockReason(Messages.format(ALREADY_PRINTED, ticket.getTicketCode(),
                        ticket.getPrintedAt() == null ? "" : ticket.getPrintedAt().format(TIME),
                        ticket.getPrintedBy() == null ? "" : ticket.getPrintedBy()));
                ticket.setReprintable(bookingBlock == null);
            } else if (booking.isCounterSale()) {
                ticket.setBlockReason(COUNTER_PRINTED);
                ticket.setReprintable(bookingBlock == null);
            } else {
                ticket.setPrintable(bookingBlock == null);
                ticket.setBlockReason(bookingBlock);
            }
        }
    }

    // A7 / BR-36-09: các suất đang mở cửa soát vé để nhân viên chọn suất mình đang soát
    public List<DoorShowtime> doorShowtimes() throws SQLException {
        return printDAO.findDoorShowtimes(LocalDateTime.now(), DOOR_WINDOW_MINUTES, ShowtimeStatus.ACTIVE,
                BookingStatus.PAID, TicketStatus.UPCOMING);
    }

    // A7: soát một vé tại cửa phòng chiếu. Hợp lệ thì ghi lượt vào cửa (UNIQUE TicketID nên hai cửa quét cùng lúc chỉ
    // một lượt được ghi), không hợp lệ thì trả lý do và không ghi gì (E10-E13). suspicious = vé đã dùng / vé giấy cũ,
    // servlet ghi cảnh báo vào nhật ký (UC-32).
    public CheckInResult checkIn(String raw, int showtimeId, int staffId) throws SQLException, ValidationException {
        // BR-36-07: người soát cửa không phải thu ngân nên không cần mở ca; đang có ca thì ghi kèm ca để đối chiếu
        Shift shift = shiftService.findOpenShift(staffId);
        DoorShowtime door = doorShowtimes().stream().filter(s -> s.getShowtimeId() == showtimeId).findFirst()
                .orElseThrow(() -> error(DOOR_NO_SHOWTIME));
        String text = raw == null ? "" : raw.toUpperCase(Locale.ROOT);
        String code = extractCode(text);
        int ticketId = code.isEmpty() ? 0 : printDAO.findTicketIdByCode(code);
        PrintBooking booking = ticketId > 0 ? printDAO.findBooking(printDAO.findBookingIdByTicket(ticketId)) : null;
        PrintTicket ticket = booking == null ? null : booking.getTickets().stream()
                .filter(t -> t.getTicketId() == ticketId).findFirst().orElse(null);
        if (ticket == null) {
            return new CheckInResult(false, NOT_FOUND, null, null, null, 0, false);
        }
        // Mã không có số lần in (QR vé điện tử, gõ mã vé, vé bán tại quầy) tính là lần in 1
        Matcher paper = PAPER_CODE.matcher(text);
        int scannedPrint = paper.find() ? Integer.parseInt(paper.group(1)) : 1;
        int latestPrint = Math.max(1, lastPrintNo(booking, ticket.getPrintCount()));
        LocalDateTime now = LocalDateTime.now();
        String reason = null;
        boolean suspicious = false;
        if (!BookingStatus.PAID.equals(booking.getBookingStatus())) {
            reason = NOT_PAID;
        } else if (TicketStatus.CANCELLED.equals(ticket.getStatus())) {
            reason = CANCELLED;
        } else if (TicketStatus.EXCHANGED.equals(ticket.getStatus())) {
            reason = ticket.getExchangedCode() == null ? EXCHANGED_UNKNOWN
                    : Messages.format(EXCHANGED, ticket.getExchangedSeat(), ticket.getExchangedCode());
        } else if (ticket.isCheckedIn()) {
            reason = usedMessage(ticket);
            suspicious = true;
        } else if (ticket.getShowtimeId() != showtimeId) {
            reason = Messages.format(DOOR_WRONG_SHOWTIME, booking.getStartTime().format(TIME), booking.getRoomName());
        } else if (!TicketStatus.UPCOMING.equals(ticket.getStatus())
                || ShowtimeStatus.CANCELLED.equals(booking.getShowtimeStatus())
                || ShowtimeStatus.ENDED.equals(booking.getShowtimeStatus()) || !now.isBefore(booking.getEndTime())) {
            reason = ENDED;
        } else if (now.isBefore(booking.getStartTime().minusMinutes(DOOR_WINDOW_MINUTES))) {
            reason = Messages.format(DOOR_TOO_EARLY,
                    booking.getStartTime().minusMinutes(DOOR_WINDOW_MINUTES).format(TIME));
        } else if (RoomStatus.MAINTENANCE.equals(ticket.getSeatStatus())) {
            reason = Messages.format(BROKEN_SEAT, ticket.getSeatLabel());
        } else if (scannedPrint < latestPrint) {
            // E11: vé giấy cũ (hoặc vé điện tử) của vé đã in lại
            reason = Messages.format(DOOR_OLD_PRINT, String.valueOf(latestPrint - 1));
            suspicious = true;
        } else if (scannedPrint > latestPrint) {
            reason = Messages.format(DOOR_BAD_PRINT, String.valueOf(scannedPrint));
            suspicious = true;
        }
        if (reason != null) {
            return rejected(reason, booking, ticket, suspicious);
        }
        try {
            printDAO.insertCheckIn(ticketId, scannedPrint, staffId, shift == null ? null : shift.getShiftId());
        } catch (SQLException e) {
            if (!isDuplicate(e)) {
                throw e;
            }
            // E10: cửa khác vừa cho vé này vào (bản gốc và bản photo quét cùng lúc)
            PrintBooking fresh = printDAO.findBooking(booking.getBookingId());
            PrintTicket again = fresh == null ? null : fresh.getTickets().stream()
                    .filter(t -> t.getTicketId() == ticketId).findFirst().orElse(null);
            return rejected(again != null && again.isCheckedIn() ? usedMessage(again)
                    : Messages.format(DOOR_USED, ticket.getTicketCode(), "", ""), booking, ticket, true);
        }
        return new CheckInResult(true, Messages.format(DOOR_ADMITTED, ticket.getSeatLabel(), door.getRoomName()),
                ticket.getTicketCode(), ticket.getSeatLabel(), detail(booking), booking.getBookingId(), false);
    }

    private String usedMessage(PrintTicket ticket) {
        return Messages.format(DOOR_USED, ticket.getTicketCode(), ticket.getCheckedInAt().format(TIME),
                ticket.getCheckedInBy() == null ? "" : ticket.getCheckedInBy());
    }

    private CheckInResult rejected(String reason, PrintBooking booking, PrintTicket ticket, boolean suspicious) {
        return new CheckInResult(false, reason, ticket.getTicketCode(), ticket.getSeatLabel(), detail(booking),
                booking.getBookingId(), suspicious);
    }

    private String detail(PrintBooking booking) {
        return booking.getMovieTitle() + " · " + booking.getStartTime().format(TIME) + " · " + booking.getRoomName();
    }

    // Ảnh QR soát cửa nhúng thẳng vào vé giấy
    private String qrDataUri(String content) {
        try {
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(QrCodeImage.png(content, QR_SIZE));
        } catch (IOException e) {
            // Không tạo được ảnh thì vé vẫn in, nhân viên cửa gõ mã in dưới ô QR
            return null;
        }
    }

    private ValidationException alreadyPrinted(int bookingId, int ticketId) throws SQLException {
        PrintBooking fresh = printDAO.findBooking(bookingId);
        if (fresh != null) {
            evaluate(fresh, LocalDateTime.now());
            for (PrintTicket ticket : fresh.getTickets()) {
                if (ticket.getTicketId() == ticketId && ticket.getBlockReason() != null) {
                    return error(ticket.getBlockReason());
                }
            }
        }
        return error(Messages.format(ALREADY_PRINTED, "", "", ""));
    }

    private boolean isDuplicate(SQLException e) {
        return e.getErrorCode() == SQL_UNIQUE_VIOLATION || e.getErrorCode() == SQL_UNIQUE_INDEX_VIOLATION;
    }

    private ValidationException error(String message) {
        return new ValidationException(Map.of("general", message));
    }
}
