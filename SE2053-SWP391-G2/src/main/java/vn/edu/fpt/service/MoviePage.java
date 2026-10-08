package vn.edu.fpt.service;

import java.util.List;
import java.util.Map;
import vn.edu.fpt.model.Movie;

public class MoviePage {
    private final List<Movie> movies;
    private final int page;
    private final int pageSize;
    private final int totalItems;
    private final Map<String, Integer> statusCounts;

    public MoviePage(List<Movie> movies, int page, int pageSize, int totalItems, Map<String, Integer> statusCounts) {
        this.movies = movies;
        this.page = page;
        this.pageSize = pageSize;
        this.totalItems = totalItems;
        this.statusCounts = statusCounts;
    }

    public List<Movie> getMovies() {
        return movies;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
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

    public Map<String, Integer> getStatusCounts() {
        return statusCounts;
    }

    public int getAllCount() {
        return statusCounts.values().stream().mapToInt(Integer::intValue).sum();
    }
}
