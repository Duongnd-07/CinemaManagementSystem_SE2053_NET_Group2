package vn.edu.fpt.service;

public class AnnouncementPreview {
    private final int recipientCount;
    private final String customerName;
    private final String maskedEmail;
    private final String targetLabel;
    private final String typeLabel;

    public AnnouncementPreview(int recipientCount, String customerName, String maskedEmail,
            String targetLabel, String typeLabel) {
        this.recipientCount = recipientCount;
        this.customerName = customerName;
        this.maskedEmail = maskedEmail;
        this.targetLabel = targetLabel;
        this.typeLabel = typeLabel;
    }

    public int getRecipientCount() { return recipientCount; }
    public String getCustomerName() { return customerName; }
    public String getMaskedEmail() { return maskedEmail; }
    public String getTargetLabel() { return targetLabel; }
    public String getTypeLabel() { return typeLabel; }
}
