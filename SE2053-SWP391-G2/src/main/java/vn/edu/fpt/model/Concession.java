package vn.edu.fpt.model;

import java.math.BigDecimal;

public class Concession {
    private int concessionId;
    private String name;
    private BigDecimal price;
    private int stockQuantity;
    private String status;
    private String description;
    private String imageUrl;

    public Concession() {
    }

    public int getConcessionId() {
        return concessionId;
    }

    public void setConcessionId(int concessionId) {
        this.concessionId = concessionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
