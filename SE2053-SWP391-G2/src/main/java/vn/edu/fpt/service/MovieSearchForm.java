package vn.edu.fpt.service;

// UC-05: tiêu chí tìm kiếm/lọc phim do người dùng nhập; Service chuẩn hóa trước khi truy vấn và hiển thị lại
public class MovieSearchForm {
    private String keyword = "";
    private String genreId = "";
    private String director = "";
    private String cast = "";
    private String ageRating = "";

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getGenreId() {
        return genreId;
    }

    public void setGenreId(String genreId) {
        this.genreId = genreId;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public String getCast() {
        return cast;
    }

    public void setCast(String cast) {
        this.cast = cast;
    }

    public String getAgeRating() {
        return ageRating;
    }

    public void setAgeRating(String ageRating) {
        this.ageRating = ageRating;
    }

    // Không có từ khóa và không có bộ lọc nào: trả về toàn bộ danh sách
    public boolean isEmpty() {
        return keyword.isEmpty() && genreId.isEmpty() && director.isEmpty() && cast.isEmpty() && ageRating.isEmpty();
    }
}
