package vn.edu.fpt.util;

import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

// Poster upload được lưu ngoài thư mục deploy (không mất khi redeploy/clean), PosterServlet phục vụ lại theo POSTER_DIR
public final class PosterStorage {
    public static final String POSTER_DIR = "assets/images/posters/";
    public static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final Logger LOGGER = Logger.getLogger(PosterStorage.class.getName());
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "jpg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp");
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    // Tên file do server sinh (UUID + đuôi), chỉ chấp nhận đúng dạng này để chặn path traversal
    private static final Pattern STORED_PATH = Pattern.compile(
            "^" + Pattern.quote(POSTER_DIR) + "[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}\\.(jpg|png|webp)$");
    private static final Path STORAGE_DIR = resolveStorageDir();

    private PosterStorage() {
    }

    public static boolean hasFile(Part part) {
        return part != null && part.getSize() > 0;
    }

    // Trả về thông báo lỗi, hoặc null nếu file hợp lệ
    public static String validate(Part part) throws IOException {
        if (part.getSize() > MAX_SIZE) {
            return Messages.MOVIE_POSTER_FILE_TOO_LARGE;
        }
        if (detectExtension(part) == null) {
            return Messages.MOVIE_POSTER_FILE_INVALID;
        }
        return null;
    }

    public static String store(Part part) throws IOException {
        String extension = detectExtension(part);
        if (extension == null) {
            throw new IOException("File poster không phải ảnh JPG/PNG/WEBP");
        }
        Files.createDirectories(STORAGE_DIR);
        String fileName = UUID.randomUUID() + "." + extension;
        try (InputStream in = part.getInputStream()) {
            Files.copy(in, STORAGE_DIR.resolve(fileName));
        }
        return POSTER_DIR + fileName;
    }

    public static boolean isStoredPath(String path) {
        return path != null && STORED_PATH.matcher(path).matches();
    }

    // File thật của poster đã lưu, hoặc null nếu đường dẫn không do hệ thống sinh (URL ngoài, dữ liệu sai)
    public static Path resolve(String storedPath) {
        return isStoredPath(storedPath) ? STORAGE_DIR.resolve(storedPath.substring(POSTER_DIR.length())) : null;
    }

    public static String contentType(Path file) {
        String name = file.getFileName().toString();
        return CONTENT_TYPES.get(name.substring(name.lastIndexOf('.') + 1));
    }

    public static void deleteQuietly(String storedPath) {
        Path file = resolve(storedPath);
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Không xóa được file poster " + storedPath, e);
        }
    }

    // Nhận dạng ảnh theo magic bytes, không tin Content-Type/tên file do trình duyệt gửi
    private static String detectExtension(Part part) throws IOException {
        byte[] head = new byte[12];
        int read;
        try (InputStream in = part.getInputStream()) {
            read = in.readNBytes(head, 0, head.length);
        }
        if (read >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (read >= PNG_SIGNATURE.length && Arrays.equals(Arrays.copyOf(head, PNG_SIGNATURE.length), PNG_SIGNATURE)) {
            return "png";
        }
        if (read >= 12 && "RIFF".equals(new String(head, 0, 4, StandardCharsets.US_ASCII))
                && "WEBP".equals(new String(head, 8, 4, StandardCharsets.US_ASCII))) {
            return "webp";
        }
        return null;
    }

    private static Path resolveStorageDir() {
        String configured = AppConfig.get("upload.posterDir", "");
        return configured.isEmpty()
                ? Path.of(System.getProperty("user.home"), "cinema-uploads", "posters")
                : Path.of(configured);
    }
}
