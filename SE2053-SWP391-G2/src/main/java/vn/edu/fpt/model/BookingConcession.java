package vn.edu.fpt.model;

import java.math.BigDecimal;

public class BookingConcession {
    private int bookingId;
    private int concessionId;
    private int quantity;
    private BigDecimal subtotalPrice;

    public BookingConcession() {
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public int getConcessionId() {
        return concessionId;
    }

    public void setConcessionId(int concessionId) {
        this.concessionId = concessionId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getSubtotalPrice() {
        return subtotalPrice;
    }

    public void setSubtotalPrice(BigDecimal subtotalPrice) {
        this.subtotalPrice = subtotalPrice;
    }
}
