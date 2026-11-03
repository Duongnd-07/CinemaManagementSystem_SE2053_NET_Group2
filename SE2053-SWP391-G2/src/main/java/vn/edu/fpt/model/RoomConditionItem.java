package vn.edu.fpt.model;

// UC-35: kết quả một hạng mục trong báo cáo tình trạng phòng
public class RoomConditionItem {
    private String itemKey;
    private boolean ok;
    private String note;

    public RoomConditionItem() {
    }

    public RoomConditionItem(String itemKey, boolean ok, String note) {
        this.itemKey = itemKey;
        this.ok = ok;
        this.note = note;
    }

    public String getItemKey() {
        return itemKey;
    }

    public void setItemKey(String itemKey) {
        this.itemKey = itemKey;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getLabel() {
        RoomCheckItem item = RoomCheckItem.fromKey(itemKey);
        return item == null ? itemKey : item.getLabel();
    }
}
