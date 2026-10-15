package vn.edu.fpt.model;

// UC-25: một dòng trong danh sách phòng chiếu - phòng kèm số ghế và số suất chiếu sắp tới
public class RoomSummary {
    private Room room;
    private int activeSeats;
    private int disabledSeats;
    private int upcomingShowtimes;

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public int getActiveSeats() {
        return activeSeats;
    }

    public void setActiveSeats(int activeSeats) {
        this.activeSeats = activeSeats;
    }

    public int getDisabledSeats() {
        return disabledSeats;
    }

    public void setDisabledSeats(int disabledSeats) {
        this.disabledSeats = disabledSeats;
    }

    public int getUpcomingShowtimes() {
        return upcomingShowtimes;
    }

    public void setUpcomingShowtimes(int upcomingShowtimes) {
        this.upcomingShowtimes = upcomingShowtimes;
    }
}
