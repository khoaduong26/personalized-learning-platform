# Hướng dẫn AI và cộng tác viên

## Mục tiêu dự án

Xây dựng nền tảng học tập số cá nhân hóa cho học sinh tiểu học, tập trung vào Toán lớp 2. Hệ thống hỗ trợ giáo viên tổ chức lớp và đánh giá, phân tích năng lực học sinh, xác định khoảng trống kiến thức, đề xuất lộ trình học và tạo bài tập để giáo viên duyệt.

## Nguồn yêu cầu

- Đọc [`TLCN.md`](./TLCN.md) trước khi thực hiện thay đổi liên quan đến nghiệp vụ, dữ liệu hoặc kiến trúc.
- `TLCN.md` là nguồn chuẩn cho 35 use case, mô hình dữ liệu dự kiến và lộ trình triển khai. Giữ nguyên ý nghĩa nghiệp vụ trong tài liệu đó.
- Nếu yêu cầu mới mâu thuẫn với `TLCN.md` hoặc mã hiện có, nêu rõ điểm mâu thuẫn và chọn phương án ít ảnh hưởng nhất; không âm thầm thay đổi phạm vi sản phẩm.
- Phân biệt kiến trúc mục tiêu trong `TLCN.md` với trạng thái triển khai thực tế. Kiểm tra mã nguồn và cấu hình trước khi quyết định công nghệ hoặc cấu trúc.

## Quy tắc làm việc

1. Trước khi sửa, xem các tệp liên quan và cách dự án đang tổ chức mã. Thực hiện thay đổi nhỏ, nhất quán với quy ước hiện có.
2. Không tự ý thêm thư viện, dịch vụ bên ngoài, thông tin bí mật, dữ liệu thật của học sinh hoặc cấu hình triển khai không được yêu cầu.
3. Bảo vệ dữ liệu cá nhân của học sinh. Áp dụng phân quyền theo vai trò (`ADMIN`, `TEACHER`, `STUDENT`) và kiểm tra quyền ở backend đối với dữ liệu lớp, hồ sơ, bài làm và lộ trình.
4. Với nghiệp vụ AI, đầu vào và kết quả cần gắn với học sinh, bài làm và đơn vị kiến thức phù hợp; coi kết quả sinh bài tập là `PENDING` cho đến khi giáo viên duyệt.
5. Giữ các phép tính điểm, chấm bài và cập nhật tiến độ có thể kiểm tra được. Không để AI tự quyết định kết quả chấm điểm nếu không có quy tắc xác định trong yêu cầu.
6. Cập nhật tài liệu khi thay đổi API, mô hình dữ liệu, cách chạy hoặc hành vi sản phẩm.
7. Không xóa hoặc ghi đè thay đổi sẵn có của người dùng. Tránh thay đổi phá vỡ tương thích nếu yêu cầu không đòi hỏi.

## Định hướng kỹ thuật

- Kiến trúc sản phẩm hướng tới client–server, API-first, backend phân lớp Controller → Service/DTO → Repository.
- `TLCN.md` đề xuất frontend React/Next.js với TypeScript và Tailwind CSS; backend Java/Spring Boot; MySQL; Cloudinary; dịch vụ AI; Socket.IO; Docker.
- Các đề xuất trên không khẳng định cấu hình hiện tại đã dùng các công nghệ đó. Hãy đọc `package.json`, `pom.xml`, cấu hình và mã liên quan trước khi chọn giải pháp. Không chuyển đổi framework hoặc ngôn ngữ nếu nhiệm vụ không yêu cầu.
- Giữ API rõ ràng, xác thực và phân quyền tại backend; không đưa khóa bí mật vào frontend hoặc kho mã nguồn.
- Gắn câu hỏi với `Lesson` để hỗ trợ phân tích năng lực và khoảng trống kiến thức. Dùng trạng thái rõ ràng cho lượt làm bài, lộ trình, bước học và duyệt bài tập.

## Tiêu chí hoàn thành

- Chỉ triển khai phạm vi được yêu cầu, nhất quán với nghiệp vụ liên quan trong `TLCN.md`.
- Giữ giao diện học sinh tiểu học dễ hiểu, thao tác đơn giản và phản hồi rõ ràng.
- Tóm tắt tệp đã thay đổi, hành vi được cập nhật và mọi giả định hoặc giới hạn còn lại khi bàn giao.
- Chỉ chạy kiểm thử hoặc xác minh tự động khi được yêu cầu; nếu không, không tuyên bố rằng đã kiểm thử.
