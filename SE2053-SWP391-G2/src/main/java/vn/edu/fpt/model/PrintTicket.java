package vn.edu.fpt.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// UC-36: một vé của đơn trên màn Quét & in vé, kèm lần in gần nhất. printable/blockReason do TicketPrintService tính.
// Cũng dùng làm một vé giấy trong lượt in (printNo, printedAt, printedBy là của lần in đó).
public class PrintTicket implements Serializable {
    private static final long serialVersionUID = 1L;

    private int ticketId;
    private String seatLabel;
    private String seatType;
    private BigDecimal price;
    private String ticketCode;
    private String status;
    // Số lần in đã có (PrintNo lớn nhất; 0 = chưa in)
    private int printCount;
    private LocalDateTime printedAt;
    private String printedBy;
    // Vé thay thế khi đã đổi ghế (UC-20)
    private String exchangedSeat;
    private String exchangedCode;

    private boolean printable;
    private boolean reprintable;
    private String blockReason;

    public int getTicketId() {
        return ticketId;
    }

    public void setTicketId(int ticketId) {
        this.ticketId = ticketId;
    }

    public String getSeatLabel() {
        return seatLabel;
    }

    public void setSeatLabel(String seatLabel) {
        this.seatLabel = seatLabel;
    }

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public void setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getPrintCount() {
        return printCount;
    }

    public void setPrintCount(int printCount) {
        this.printCount = printCount;
    }

    public LocalDateTime getPrintedAt() {
        return printedAt;
    }

    public void setPrintedAt(LocalDateTime printedAt) {
        this.printedAt = printedAt;
    }

    public String getPrintedBy() {
        return printedBy;
    }

    public void setPrintedBy(String printedBy) {
        this.printedBy = printedBy;
    }

    public String getExchangedSeat() {
        return exchangedSeat;
    }

    public void setExchangedSeat(String exchangedSeat) {
        this.exchangedSeat = exchangedSeat;
    }

    public String getExchangedCode() {
        return exchangedCode;
    }

    public void setExchangedCode(String exchangedCode) {
        this.exchangedCode = exchangedCode;
    }

    public boolean isPrintable() {
        return printable;
    }

    public void setPrintable(boolean printable) {
        this.printable = printable;
    }

    public boolean isReprintable() {
        return reprintable;
    }

    public void setReprintable(boolean reprintable) {
        this.reprintable = reprintable;
    }

    public String getBlockReason() {
        return blockReason;
    }

    public void setBlockReason(String blockReason) {
        this.blockReason = blockReason;
    }

    // Lần in lại thứ mấy (lần in đầu là 0)
    public int getReprintNo() {
        return Math.max(0, printCount - 1);
    }
}
