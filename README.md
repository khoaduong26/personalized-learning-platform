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

`POST /api/exercises/steps/{stepId}/generate` tạo câu hỏi trắc nghiệm Toán lớp 2 bằng Gemini hoặc DeepSeek. Cấu hình `APP_EXERCISE_GENERATOR_PROVIDER=gemini|deepseek` cùng khóa và model tương ứng trong môi trường backend (`GEMINI_API_KEY`/`GEMINI_MODEL` hoặc `DEEPSEEK_API_KEY`/`DEEPSEEK_MODEL`). Mặc định vẫn là Gemini. Khi Gemini hết hạn mức/quá tải/lỗi kết nối, backend có thể tự chuyển sang DeepSeek nếu `APP_AI_FALLBACK_PROVIDER=deepseek` (mặc định) và `DEEPSEEK_API_KEY` đã cấu hình. Không đưa khóa thật vào Git hoặc frontend. Mọi bài tập do AI sinh đều ở trạng thái `PENDING` để giáo viên rà soát và duyệt; kết quả model phải qua validation backend.

Nếu AI không khả dụng, giáo viên có thể tự soạn câu trắc nghiệm, điền khuyết hoặc trả lời ngắn ngay trên từng bước của lộ trình. Bài thủ công được liên kết đúng với bài học của bước, chờ duyệt rồi mới xuất hiện với học sinh.

Backend luôn tạo lộ trình theo quy tắc ưu tiên của khoảng trống kiến thức. Gemini hoặc DeepSeek chỉ đề xuất thứ tự trong cùng một mức ưu tiên; backend từ chối đề xuất thiếu/trùng bước hoặc đảo thứ tự ưu tiên. Nếu AI lỗi hoặc đề xuất không hợp lệ, backend vẫn lưu lộ trình theo thứ tự xác định: mức ưu tiên, thời điểm phát hiện, rồi mã bài học. Cấu hình provider bằng `APP_LEARNING_PATH_GENERATOR_PROVIDER=gemini|deepseek`; Gemini có thể fallback qua DeepSeek theo `APP_AI_FALLBACK_PROVIDER` nếu khóa DeepSeek đã cấu hình. DeepSeek dùng `https://api.deepseek.com/chat/completions`, model mặc định `deepseek-flash`.

### Xem bài làm của học sinh

Trong **Hồ sơ năng lực** của từng học sinh, giáo viên có thể mở từng câu trả lời sai để xem nội dung câu hỏi, câu trả lời của học sinh, đáp án đúng, điểm câu hỏi và lời giải (nếu có). Quyền xem vẫn được giới hạn theo lớp mà giáo viên quản lý.

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

Seeder chỉ thêm dữ liệu tổng hợp khi bật đồng thời profile `demo` và `app.demo-seed.enabled=true`. Chỉ chạy trên database phát triển, không dùng database production hoặc nơi có dữ liệu học sinh thật. Từ thư mục `backend`, chạy trong PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'demo'
$env:APP_DEMO_SEED_ENABLED = 'true'
.\mvnw.cmd spring-boot:run
```

Tài khoản demo dùng mật khẩu `Demo123!`: giáo viên `teacher.demo@example.test`, học sinh `student.an@example.test` và `student.binh@example.test`. Nếu tài khoản giáo viên demo đã tồn tại, seeder bỏ qua toàn bộ dữ liệu và không xóa hoặc ghi đè dữ liệu có sẵn. Bỏ hai biến bật demo trước lần chạy bình thường tiếp theo.

1. Đăng nhập bằng tài khoản giáo viên demo, mở lớp để xem hồ sơ năng lực/lộ trình của học sinh.
2. Mở **Kiểm tra và đánh giá** để xem bài đã giao, lịch sử và kết quả.
3. Mở **Duyệt bài tập** để kiểm tra nội dung/đáp án, chỉnh sửa, từ chối hoặc duyệt bài đang chờ.
4. Đăng nhập bằng tài khoản học sinh demo, làm bài kiểm tra hoặc bài tập đã duyệt, nộp bài và xem kết quả.
5. Quay lại tài khoản giáo viên để đối chiếu cập nhật năng lực, khoảng trống kiến thức và tiến độ lộ trình.
