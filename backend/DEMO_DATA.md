# Dữ liệu demo cho backend thật

Seeder tạo dữ liệu tổng hợp vào database đã cấu hình cho Spring Boot. Seeder mặc định tắt; chỉ chạy khi bật đồng thời Spring profile `demo` và thuộc tính `app.demo-seed.enabled=true`.

## Điều kiện

- MySQL của môi trường phát triển đang chạy và database trong `DB_URL` đã được tạo.
- Backend có thể kết nối MySQL và Hibernate tạo/cập nhật schema theo cấu hình hiện tại.
- Không dùng profile này với database production hoặc database chứa dữ liệu học sinh thật.

Ví dụ trong PowerShell, từ thư mục `backend`:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'demo'
$env:APP_DEMO_SEED_ENABLED = 'true'
.\mvnw.cmd spring-boot:run
```

Giữ `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` và `JWT_SECRET` theo cấu hình backend cục bộ. Sau khi khởi động, đăng nhập tại frontend **không có** `?mock=1` bằng một trong các tài khoản:

| Vai trò | Email | Mật khẩu |
| --- | --- | --- |
| Giáo viên | `teacher.demo@example.test` | `Demo123!` |
| Học sinh | `student.an@example.test` | `Demo123!` |
| Học sinh | `student.binh@example.test` | `Demo123!` |

## Dữ liệu được tạo

- Lớp `Lớp 2A - Demo` và hai tài khoản học sinh tổng hợp.
- Môn Toán 2, chủ đề phép cộng/trừ và giải toán có lời văn, cùng ba bài học.
- Bài khảo sát đã giao, ba câu hỏi và một lượt làm mẫu có kết quả 10/15.
- Hồ sơ thành thạo, lịch sử thay đổi, khoảng trống kiến thức và một lỗi sai liên kết với câu trả lời.
- Lộ trình hai bước cho Bình Demo, một bài tập AI chờ duyệt và một bài đã duyệt để luyện tập.

## An toàn và chạy lại

- Không có tên, email, ngày sinh hoặc câu trả lời của học sinh thật trong bộ dữ liệu này.
- Seeder không chạy chỉ vì `ddl-auto=update`; cần cả profile `demo` lẫn cờ bật rõ ràng.
- Nếu `teacher.demo@example.test` đã tồn tại, toàn bộ seed bị bỏ qua. Seeder không xóa, reset hoặc ghi đè dữ liệu hiện có.
- Tắt bằng cách bỏ `SPRING_PROFILES_ACTIVE=demo` hoặc `APP_DEMO_SEED=true` trước lần khởi động tiếp theo.

Lưu ý: dữ liệu được ghi thật vào database được cấu hình, nhưng khả năng truy cập qua giao diện vẫn phụ thuộc các route API và quyền trong `SecurityConfig`.
