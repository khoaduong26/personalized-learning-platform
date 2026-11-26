# Bộ sinh bài tập và sắp xếp lộ trình bằng Gemini hoặc DeepSeek

## Sinh bài tập

Endpoint `POST /api/exercises/steps/{stepId}/generate` nhận mã bước học. Giáo viên hoặc quản trị viên phải có quyền quản lý bước đó. Backend lấy bài học từ `LearningStep`, gửi tên và mô tả bài học tới Gemini, kiểm tra dữ liệu trả về rồi lưu câu hỏi và liên kết với bước học.

Gemini không được chọn `lessonId`; backend gắn câu hỏi vào bài học của bước. Câu hỏi mới luôn ở trạng thái `PENDING`; học sinh chỉ nhận bài sau khi giáo viên duyệt.

`GeminiExerciseQuestionGenerator.java` gọi Gemini Interactions API (`/v1beta/interactions`) và yêu cầu JSON theo schema. `DeepSeekExerciseQuestionGenerator.java` dùng DeepSeek Chat Completions JSON Output. Cả hai adapter dùng `ExerciseGeneratorSupport.java` để giới hạn prompt vào nội dung bài học và gắn `lessonId` từ backend.

## Sắp xếp lộ trình

`GeminiLearningPathOrderer.java` và `DeepSeekLearningPathOrderer.java` có thể đề xuất thứ tự cho các khoảng trống kiến thức hiện có. Backend kiểm tra danh sách vị trí trả về đầy đủ, không trùng và không có mục lạ trước khi lưu. AI không thể thêm/bỏ bài học hoặc thay đổi mục tiêu và trạng thái lộ trình.

## Cấu hình

Khi chạy Maven từ thư mục `backend/`, ứng dụng nạp `.env` trong thư mục này. Cấu hình local:

```properties
APP_EXERCISE_GENERATOR_PROVIDER=gemini
APP_LEARNING_PATH_GENERATOR_PROVIDER=gemini
APP_AI_FALLBACK_PROVIDER=deepseek
GEMINI_API_KEY=điền_khóa_gemini
GEMINI_MODEL=gemini-3.8-flash
DEEPSEEK_API_KEY=điền_khóa_deepseek
DEEPSEEK_MODEL=deepseek-flash
```

Khi provider chính là Gemini, backend tự chuyển sang DeepSeek nếu Gemini trả lỗi giới hạn tốc độ/hạn mức (HTTP 429), lỗi hạn mức hoặc thanh toán được nhận diện trong HTTP 402/403, lỗi dịch vụ HTTP 5xx hoặc lỗi kết nối. Fallback chỉ chạy khi `APP_AI_FALLBACK_PROVIDER=deepseek` và `DEEPSEEK_API_KEY` đã có. Lỗi cấu hình khóa Gemini, dữ liệu JSON không hợp lệ hoặc lỗi validation không kích hoạt fallback. Provider chính vẫn là Gemini; fallback không đổi cấu hình sang DeepSeek.

Để dùng DeepSeek cho một luồng hoặc cả hai, đặt provider tương ứng thành `deepseek` và cấu hình:

```properties
APP_EXERCISE_GENERATOR_PROVIDER=deepseek
APP_LEARNING_PATH_GENERATOR_PROVIDER=deepseek
DEEPSEEK_API_KEY=điền_khóa_deepseek
DEEPSEEK_MODEL=deepseek-flash
```

DeepSeek gọi [`/chat/completions`](https://api-docs.deepseek.com/api/create-chat-completion/) trên `https://api.deepseek.com` với [JSON Output](https://api-docs.deepseek.com/guides/json_mode/); model mặc định là `deepseek-flash`.

Mẫu cấu hình nằm trong `backend/.env.example`. Không lưu khóa thật vào Git, frontend hoặc tài liệu.
