package vn.edu.fpt.service;

import java.math.BigDecimal;

// UC-13: một món bắp nước trong đơn (hoặc món có thể chọn ở trang checkout khi quantity = 0)
public class BookingConcessionLine {
    private final int concessionId;
    private final String name;
    private final String description;
    private final String imageUrl;
    private final BigDecimal unitPrice;
    private final int maxQuantity;
    private final int quantity;

    public BookingConcessionLine(int concessionId, String name, String description, String imageUrl,
                                 BigDecimal unitPrice, int maxQuantity, int quantity) {
        this.concessionId = concessionId;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.unitPrice = unitPrice;
        this.maxQuantity = maxQuantity;
        this.quantity = quantity;
    }

    public int getConcessionId() {
        return concessionId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getMaxQuantity() {
        return maxQuantity;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
