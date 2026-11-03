package vn.edu.fpt.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import vn.edu.fpt.util.PosterStorage;

// UC-23: phục vụ poster đã upload (lưu ngoài webapp) theo cùng đường dẫn đã ghi trong Movie.PosterURL
@WebServlet("/assets/images/posters/*")
public class PosterServlet extends HttpServlet {
    private static final String CACHE_FOREVER = "public, max-age=31536000, immutable";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo();
        Path file = pathInfo == null ? null : PosterStorage.resolve(PosterStorage.POSTER_DIR + pathInfo.substring(1));
        if (file == null || !Files.isRegularFile(file)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setContentType(PosterStorage.contentType(file));
        response.setContentLengthLong(Files.size(file));
        response.setHeader("X-Content-Type-Options", "nosniff");
        // Tên file là UUID, mỗi lần đổi poster sinh file mới nên có thể cache lâu dài
        response.setHeader("Cache-Control", CACHE_FOREVER);
        Files.copy(file, response.getOutputStream());
    }
}
