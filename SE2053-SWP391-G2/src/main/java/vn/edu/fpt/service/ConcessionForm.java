package vn.edu.fpt.service;

// Dữ liệu thô (chuỗi) từ form để Service tự validate và hiển thị lại khi có lỗi
public class ConcessionForm {
    private int concessionId;
    private String name = "";
    private String description = "";
    private String price = "";
    private String stockQuantity = "";
    private String imageUrl = "";
    private String status = "";

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(String stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
