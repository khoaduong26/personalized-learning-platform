# Kiểm kê và nghiệm thu công việc ngày 22/11

**Phạm vi:** kiểm kê nhánh `develop`, đường chạy local, luồng demo, quyền API và lỗi chặn nghiệm thu.  
**Hoàn thành trên nhánh:** `codex/audit-fix-day22`.

## Kết quả kiểm kê

- Frontend đang dùng React + Vite + React Router; backend dùng Java 21, Spring Boot, JPA và MySQL. Không đổi framework.
- Các luồng đã có trong repo gồm xác thực/hồ sơ, lớp học, chương trình học, kiểm tra/chấm điểm, phân tích mastery/gap, lộ trình và bài tập chờ giáo viên duyệt. AI provider, realtime notifications và các tích hợp ngoài vẫn phụ thuộc cấu hình hoặc chưa có adapter thực tế; xem `FEATURE_ROADMAP.md`.
- Nhánh `develop` tại thời điểm bắt đầu khớp `origin/develop`; các thay đổi cục bộ có sẵn được giữ nguyên và không đưa vào các commit kiểm kê.
- README đã bổ sung hướng dẫn cấu hình MySQL/backend, chạy frontend, lệnh kiểm tra và kịch bản demo. Hướng dẫn seed chi tiết vẫn ở `backend/DEMO_DATA.md`.

## Rà soát quyền truy cập

- `SecurityConfig` chỉ mở các route có khai báo; route còn lại bị từ chối mặc định. Route lớp học, học thuật, đánh giá, hồ sơ, mastery, lộ trình và bài tập có giới hạn vai trò.
- Các service đang kiểm tra quyền theo chủ lớp/học sinh cho dữ liệu classroom, assessment, mastery, learning path và exercise. Học sinh chỉ nhận câu hỏi exercise đã `APPROVED`; response bài làm được tách khỏi đáp án chuẩn theo trạng thái nộp.
- Đã xem các integration test về xác thực và hồ sơ. Cần bổ sung integration test riêng cho ranh giới lớp/học sinh ở assessment, mastery, learning path và exercise trước khi tuyên bố toàn bộ API đã được nghiệm thu bảo mật.

## Lỗi đã sửa trong đợt kiểm kê

1. Gỡ 11 lint error do reset state đồng bộ trong `useEffect`; dependent state được reset tại hành động đổi lớp/chủ đề/bài học hoặc khi người dùng đổi học sinh. Khi đổi bài học, câu hỏi được chọn từ bài cũ cũng được bỏ chọn.
2. Chuyển khởi tạo `HttpClient` của Cloudinary sang lazy initialization. Khi thiếu thông tin Cloudinary, backend không cần tạo client mạng lúc khởi động; upload vẫn trả lỗi cấu hình rõ ràng. Thêm unit test cho trường hợp này.

## Kết quả xác minh

- Frontend: `npm test` **47/47 pass**, `npm run lint` **pass**, `npm run build` **pass**. Build hiện có cảnh báo bundle JavaScript lớn hơn 500 kB.
- Backend: `mvn --settings target/codex-test-settings.xml -o "-DargLine=-javaagent:target/m2-local/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar" test` **43/43 pass**. Settings và agent ở đây chỉ dùng cache hiện có trong workspace để chạy suite offline trên máy kiểm kê; không cần thêm cấu hình này vào repo.
- Vite báo khởi động tại `http://localhost:5173`; kiểm tra HTTP loopback trực tiếp từ shell bị hệ điều hành/sandbox từ chối socket. Vì vậy chưa ghi nhận smoke test qua browser thật trong phiên này.
- MySQL, mail, Cloudinary, Google OAuth và AI provider không được xác minh bằng tài khoản/dịch vụ thật.

## Kịch bản trình diễn

1. Seed dữ liệu theo `backend/DEMO_DATA.md`; dùng tài khoản giáo viên tổng hợp mở lớp và xem dashboard/hồ sơ năng lực.
2. Xem bài kiểm tra và lịch sử; học sinh mở bài được giao, lưu/nộp câu trả lời rồi xem kết quả.
3. Giáo viên mở trang duyệt bài, kiểm tra/sửa nội dung và đáp án rồi duyệt bài tập.
4. Học sinh mở lộ trình, làm bài tập đã duyệt và nộp bài.
5. Giáo viên đối chiếu mastery, gap và tiến độ lộ trình sau lượt làm mới.

## Điều kiện vận hành còn lại

- Tạo database MySQL và cấu hình `backend/.env` từ `.env.example` theo môi trường local; không commit `.env`.
- Chạy backend/frontend trên máy có quyền bind/connect local ports để hoàn tất smoke test đầu-cuối.
- Đưa các tích hợp AI, email OTP, Google OAuth, Cloudinary hoặc thông báo realtime vào nghiệm thu riêng khi đã có cấu hình tương ứng.
