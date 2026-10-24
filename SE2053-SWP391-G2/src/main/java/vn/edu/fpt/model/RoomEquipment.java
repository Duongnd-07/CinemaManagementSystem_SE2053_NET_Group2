package vn.edu.fpt.model;

// UC-21: một thiết bị cụ thể trong phòng chiếu (Máy chiếu 1, Điều hòa 2...), ghế không nằm ở đây
public class RoomEquipment {
    private int equipmentId;
    private int roomId;
    private String category;
    private String name;

    public RoomEquipment() {
    }

    public int getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(int equipmentId) {
        this.equipmentId = equipmentId;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
