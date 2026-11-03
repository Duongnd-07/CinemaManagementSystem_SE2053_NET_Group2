package vn.edu.fpt.model;

import java.util.List;

// Giá trị cột User.Gender, khớp ràng buộc CK_User_Gender trong UC10_profile_birthdate_gender.sql
public final class Gender {
    public static final String MALE = "Male";
    public static final String FEMALE = "Female";
    public static final String OTHER = "Other";

    public static final List<String> ALL = List.of(MALE, FEMALE, OTHER);

    private Gender() {
    }

    public static boolean isValid(String gender) {
        return ALL.contains(gender);
    }
}
