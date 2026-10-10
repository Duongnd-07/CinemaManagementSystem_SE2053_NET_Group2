package vn.edu.fpt.model;

import java.util.List;

public final class EquipmentCategory {
    public static final String PROJECTOR = "Projector";
    public static final String SOUND_SYSTEM = "Sound System";
    public static final String AIR_CONDITIONING = "Air Conditioning";
    public static final String SEAT = "Seat";
    public static final String OTHER = "Other";

    public static final List<String> ALL = List.of(PROJECTOR, SOUND_SYSTEM, AIR_CONDITIONING, SEAT, OTHER);

    private EquipmentCategory() {
    }

    public static boolean isValid(String category) {
        return ALL.contains(category);
    }
}
