package vn.edu.fpt.model;

// UC-35: các hạng mục nhân viên phải kiểm tra ở mỗi phòng trước khi mở ca
public enum RoomCheckItem {
    PROJECTOR("Máy chiếu", "Hình ảnh rõ, không nhòe hoặc lệch màn hình.", "videocam"),
    SOUND_SYSTEM("Âm thanh", "Loa hoạt động, không rè hoặc mất tiếng.", "volume_up"),
    AIR_CONDITIONING("Điều hòa", "Máy lạnh hoạt động, nhiệt độ phòng ổn định.", "ac_unit"),
    SEATS("Ghế ngồi", "Ghế không hỏng, không rách, tay vịn chắc chắn.", "event_seat"),
    CLEANLINESS("Vệ sinh", "Sàn, lối đi và hàng ghế sạch sẽ, không còn rác.", "cleaning_services");

    private final String label;
    private final String description;
    private final String icon;

    RoomCheckItem(String label, String description, String icon) {
        this.label = label;
        this.description = description;
        this.icon = icon;
    }

    // null: khóa không còn trong danh mục
    public static RoomCheckItem fromKey(String key) {
        for (RoomCheckItem item : values()) {
            if (item.name().equals(key)) {
                return item;
            }
        }
        return null;
    }

    public String getKey() {
        return name();
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }
}
