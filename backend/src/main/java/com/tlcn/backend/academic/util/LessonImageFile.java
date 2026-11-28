package com.tlcn.backend.academic.util;

import com.tlcn.backend.academic.service.AcademicBadRequestException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import org.springframework.web.multipart.MultipartFile;

public record LessonImageFile(byte[] bytes, String contentType) {
    public static final long MAX_BYTES = 2 * 1024 * 1024;

    public static LessonImageFile read(MultipartFile file) {
        if (file == null || file.isEmpty()) throw invalid("Chọn một ảnh cho bài học.");
        if (file.getSize() > MAX_BYTES) throw invalid("Ảnh bài học tối đa 2 MB.");
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
                var output = new ByteArrayOutputStream();
                if (!ImageIO.write(reader.read(0), format, output)) throw invalid("Không đọc được ảnh.");
                if (output.size() > MAX_BYTES) throw invalid("Ảnh sau xử lý vượt quá 2 MB.");
                return new LessonImageFile(output.toByteArray(), mime);
            } finally { reader.dispose(); }
        } catch (IOException | IllegalArgumentException ex) {
            throw invalid("File ảnh bị hỏng hoặc không hợp lệ.");
        }
    }

    private static AcademicBadRequestException invalid(String message) {
        return new AcademicBadRequestException(message);
    }
}
