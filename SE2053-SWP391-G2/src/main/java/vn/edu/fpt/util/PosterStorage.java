package vn.edu.fpt.util;

import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class PosterStorage {
    public static final String POSTER_DIR = "assets/images/posters/";
    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    private PosterStorage() {
    }

    public static boolean hasFile(Part part) {
        return part != null && part.getSize() > 0;
    }

    // Trả về thông báo lỗi, hoặc null nếu file hợp lệ
    public static String validate(Part part) {
        if (part.getSize() > MAX_SIZE) {
            return Messages.MOVIE_POSTER_FILE_TOO_LARGE;
        }
        if (!EXTENSIONS.containsKey(String.valueOf(part.getContentType()).toLowerCase(Locale.ROOT))) {
            return Messages.MOVIE_POSTER_FILE_INVALID;
        }
        return null;
    }

    // Tên file do server sinh để tránh path traversal và trùng tên
    public static String store(Part part, Path webappRoot) throws IOException {
        String extension = EXTENSIONS.get(part.getContentType().toLowerCase(Locale.ROOT));
        String fileName = UUID.randomUUID() + "." + extension;
        Path dir = webappRoot.resolve(POSTER_DIR);
        Files.createDirectories(dir);
        try (InputStream in = part.getInputStream()) {
            Files.copy(in, dir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        }
        return POSTER_DIR + fileName;
    }

    public static void deleteQuietly(String storedPath, Path webappRoot) {
        if (storedPath == null || !storedPath.startsWith(POSTER_DIR) || storedPath.contains("..")) {
            return;
        }
        try {
            Files.deleteIfExists(webappRoot.resolve(storedPath));
        } catch (IOException e) {
            java.util.logging.Logger.getLogger(PosterStorage.class.getName())
                    .log(java.util.logging.Level.WARNING, "Không xóa được file poster " + storedPath, e);
        }
    }
}
