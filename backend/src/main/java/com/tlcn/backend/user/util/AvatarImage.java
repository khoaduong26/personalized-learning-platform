package com.tlcn.backend.user.util;

import com.tlcn.backend.user.exception.ProfileException;
import java.io.*;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

public record AvatarImage(byte[] bytes, String contentType) {
    public static final long MAX_BYTES = 2 * 1024 * 1024;

    public static AvatarImage read(MultipartFile file) {
        if (file == null || file.isEmpty()) throw invalid("Vui lòng chọn một file ảnh.");
        if (file.getSize() > MAX_BYTES) {
            throw new ProfileException(HttpStatus.CONTENT_TOO_LARGE, "AVATAR_TOO_LARGE", "Ảnh đại diện tối đa 2 MB.");
        }
        if (!"image/jpeg".equals(file.getContentType()) && !"image/png".equals(file.getContentType())) {
            throw invalid("Chỉ hỗ trợ ảnh JPEG hoặc PNG.");
        }
        try (var source = file.getInputStream(); var input = ImageIO.createImageInputStream(source)) {
            if (input == null) throw invalid("File ảnh không hợp lệ.");
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalid("File ảnh không hợp lệ.");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                String mime = switch (format) {
                    case "jpeg", "jpg" -> "image/jpeg";
                    case "png" -> "image/png";
                    default -> throw invalid("Chỉ hỗ trợ ảnh JPEG hoặc PNG.");
                };
                if (!mime.equals(file.getContentType())) throw invalid("Định dạng ảnh không khớp nội dung file.");
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > 4096 || height > 4096) {
                    throw invalid("Kích thước ảnh tối đa 4096 × 4096 pixel.");
                }
                // Re-encode decoded pixels to remove EXIF/location metadata and trailing content.
                var output = new ByteArrayOutputStream();
                if (!ImageIO.write(reader.read(0), format, output)) throw invalid("Không đọc được ảnh.");
                if (output.size() > MAX_BYTES) {
                    throw new ProfileException(HttpStatus.CONTENT_TOO_LARGE, "AVATAR_TOO_LARGE", "Ảnh đại diện tối đa 2 MB sau xử lý.");
                }
                return new AvatarImage(output.toByteArray(), mime);
            } finally { reader.dispose(); }
        } catch (IOException | IllegalArgumentException ex) {
            throw invalid("File ảnh bị hỏng hoặc không hợp lệ.");
        }
    }

    private static ProfileException invalid(String message) {
        return new ProfileException(HttpStatus.BAD_REQUEST, "AVATAR_INVALID", message);
    }
}
