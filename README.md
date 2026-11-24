# TLCN — Nền tảng học tập cá nhân hóa

Ứng dụng web hỗ trợ giáo viên đánh giá năng lực và đề xuất nội dung học tập phù hợp cho từng học sinh tiểu học, tập trung vào **môn Toán lớp 2**.

Dự án hướng đến một quá trình học có sự đồng hành của giáo viên: từ đánh giá kiến thức, nhận biết phần học sinh cần củng cố đến đề xuất lộ trình và luyện tập theo năng lực.

## 1. Mục tiêu và đối tượng sử dụng

### Mục tiêu

- Giúp học sinh học theo nhịp độ riêng, củng cố kiến thức còn yếu và theo dõi sự tiến bộ.
- Hỗ trợ giáo viên nhận biết năng lực của từng em dựa trên kết quả học tập.
- Đề xuất lộ trình và bài tập phù hợp với những kiến thức học sinh cần luyện thêm.
- Giữ vai trò kiểm soát của giáo viên: bài tập do AI đề xuất cần được giáo viên duyệt trước khi giao.

### Đối tượng sử dụng

| Đối tượng | Vai trò trong ứng dụng |
| --- | --- |
| Học sinh | Làm bài kiểm tra, luyện tập và theo dõi kết quả học tập. Giao diện ưu tiên câu chữ dễ hiểu, nút dễ bấm và phản hồi khích lệ. |
| Giáo viên | Quản lý lớp, đánh giá học sinh, xem năng lực và duyệt bài tập được đề xuất. |
| Quản trị viên | Quản lý tài khoản, vai trò và dữ liệu học thuật theo quyền được cấp. |

## Chạy ở môi trường phát triển

### Yêu cầu

- Java 21 và Maven.
- Node.js/npm tương thích với phiên bản Vite trong `frontend/package.json`.
- MySQL đang chạy và một database rỗng, ví dụ `tlcn_db`.

### Cấu hình backend

Từ thư mục `backend`, nếu chưa có `.env`, tạo file đó từ `.env.example` rồi cập nhật thông tin MySQL. `.env` bị loại khỏi Git; không đưa thông tin đăng nhập hoặc khóa dịch vụ vào README hay commit.

Các biến cấu hình chính gồm `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_DDL_AUTO`, `FRONTEND_URL`, `JWT_SECRET` và các biến email/Cloudinary/Google OAuth khi cần bật các tích hợp đó. Khi chưa có cấu hình Cloudinary, chức năng tải ảnh đại diện trả thông báo dịch vụ chưa khả dụng; backend vẫn có thể khởi động.

### Sinh bài tập

`POST /api/exercises/steps/{stepId}/generate` tạo một câu hỏi minh họa bằng bộ quy tắc demo mặc định. Hiện chỉ có provider `demo` (`APP_EXERCISE_GENERATOR_PROVIDER=demo`); phản hồi ghi rõ `provider`, `demo` và lưu ý rằng đây chưa phải nội dung do AI bên ngoài sinh. Adapter khác cần triển khai contract `ExerciseQuestionGenerator`; dự án chưa cấu hình hoặc gọi nhà cung cấp AI bên ngoài. Bài tập luôn gắn với bài học của bước đã chọn, ở trạng thái `PENDING` để giáo viên rà soát và duyệt.

Tạo database trước khi chạy, ví dụ:

```sql
CREATE DATABASE tlcn_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Mở terminal tại `backend` và chạy:

```powershell
mvn spring-boot:run
```

### Chạy frontend

Mở terminal khác tại `frontend`:

```powershell
npm ci
npm run dev
```

Mở địa chỉ Vite hiển thị trong terminal (mặc định `http://localhost:5173`). Các request `/api`, `/oauth2` và `/login/oauth2` được Vite chuyển tiếp tới backend ở `http://localhost:8080`.

### Kiểm tra

Tại `frontend`:

```powershell
npm test
npm run lint
npm run build
```

Tại `backend`:

```powershell
mvn test
```

Backend test dùng H2 trong bộ nhớ qua profile `test`, không cần MySQL. Nếu môi trường Maven cần cấu hình cache riêng, dùng Maven settings phù hợp trên máy đó; không thêm đường dẫn cache cá nhân vào cấu hình dự án.

## Kịch bản demo

Để chạy dữ liệu tổng hợp, xem [hướng dẫn seed demo](./backend/DEMO_DATA.md). Seed chỉ chạy khi bật rõ profile `demo` và `app.demo-seed.enabled=true`; không dùng database production hoặc database có dữ liệu học sinh thật.

1. Đăng nhập bằng tài khoản giáo viên demo, mở lớp để xem hồ sơ năng lực/lộ trình của học sinh.
2. Mở **Kiểm tra và đánh giá** để xem bài đã giao, lịch sử và kết quả.
3. Mở **Duyệt bài tập** để kiểm tra nội dung/đáp án, chỉnh sửa, từ chối hoặc duyệt bài đang chờ.
4. Đăng nhập bằng tài khoản học sinh demo, làm bài kiểm tra hoặc bài tập đã duyệt, nộp bài và xem kết quả.
5. Quay lại tài khoản giáo viên để đối chiếu cập nhật năng lực, khoảng trống kiến thức và tiến độ lộ trình.
