package vn.edu.fpt.service;

import java.util.List;
import vn.edu.fpt.model.Genre;

public class GenrePage {
    private final List<Genre> genres;
    private final int page;
    private final int pageSize;
    private final int totalItems;

    public GenrePage(List<Genre> genres, int page, int pageSize, int totalItems) {
        this.genres = genres;
        this.page = page;
        this.pageSize = pageSize;
        this.totalItems = totalItems;
    }

    public List<Genre> getGenres() {
        return genres;
    }

    public int getPage() {
        return page;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getTotalPages() {
        return Math.max(1, (totalItems + pageSize - 1) / pageSize);
    }

    public int getFromItem() {
        return totalItems == 0 ? 0 : (page - 1) * pageSize + 1;
    }

    public int getToItem() {
        return Math.min(totalItems, page * pageSize);
    }
}
