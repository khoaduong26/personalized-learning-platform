# Lộ trình triển khai theo feature nghiệp vụ

Tài liệu này chia 35 Use Case trong [`TLCN.md`](./TLCN.md) thành các feature nghiệp vụ độc lập và sắp xếp theo mức độ ưu tiên cùng quan hệ phụ thuộc. Dùng cùng [`AGENTS.md`](./AGENTS.md) và [`PROMPT_LO_TRINH_XAY_DUNG_WEB.md`](./PROMPT_LO_TRINH_XAY_DUNG_WEB.md) khi bắt đầu triển khai.

## Nguyên tắc chia feature

- Mỗi feature sở hữu nghiệp vụ và model của nó; các lớp liên quan nằm cùng package theo cấu trúc **package-by-feature**.
- Trong package nghiệp vụ, tổ chức các tầng `controller`, `service`, `repository`, `entity` (Model/Entity) và `dto` khi cần. Controller gọi Service; Service thực hiện nghiệp vụ và gọi Repository; không truy cập Repository trực tiếp từ Controller.
- `common` và `config` chỉ dành cho thành phần hạ tầng dùng chung, không gom toàn bộ controller/service/repository hoặc model vào đây.
- Các package nghiệp vụ dự kiến: `auth`, `user`, `academic`, `classroom`, `assessment`, `mastery`, `learningpath`, `exercise`, `ai`.
- Dùng lại model giữa các feature qua ranh giới package đã xác định. Ví dụ `Question` thuộc `assessment`; feature `exercise` liên kết và tái sử dụng câu hỏi đó, không tạo bản sao.
- Ưu tiên hoàn thiện một luồng dọc có thể dùng được từ API đến giao diện trước khi mở rộng đầy đủ các thao tác phụ.

## Thứ tự ưu tiên tổng thể

| Ưu tiên | Feature | Package chính | Use Case | Phụ thuộc chính |
|---|---|---|---|---|
| P0 | Nền tảng kỹ thuật và quy ước | `common`, `config` | Nền tảng | — |
| P1 | Tài khoản, xác thực và vai trò | `auth`, `user` | UC01–UC03, UC07 | P0 |
| P2 | Chương trình học | `academic` | UC06 | P0, cần người dùng có quyền quản trị |
| P3 | Lớp học và danh sách học sinh | `classroom` | UC04–UC05 | P1 |
| P4 | Ngân hàng câu hỏi, kiểm tra và chấm điểm | `assessment` | UC08–UC12 | P1–P3 |
| P5 | Phân tích năng lực và khoảng trống kiến thức | `mastery`, `ai` | UC13–UC19 | P2, P4 |
| P6 | Lộ trình học tập cá nhân hóa | `learningpath` | UC20–UC25 | P5 |
| P7 | Bài tập cá nhân hóa và giáo viên duyệt | `exercise`, `ai` | UC26–UC31 | P2, P4, P6 |
| P8 | Đánh giá lại và khép kín vòng học tập | `mastery`, `learningpath`, `ai` | UC32–UC35 | P5–P7 |
| P9 | Hoàn thiện, triển khai và nghiệm thu | Toàn hệ thống | Phi chức năng | P0–P8 |

Thứ tự này là trình tự triển khai đề xuất. Có thể phát triển song song các feature không phụ thuộc trực tiếp sau khi hợp đồng API và model liên quan đã thống nhất.

---

## P0 — Nền tảng kỹ thuật và quy ước

**Mục tiêu:** Có nền chung để các feature triển khai nhất quán và kết nối được frontend–backend.

**Phạm vi công việc:**

- Kiểm kê mã hiện tại và ghi nhận khoảng cách với kiến trúc trong `TLCN.md`.
- Thống nhất package-by-feature, quy ước API/DTO, validation, lỗi API, migration database, cấu hình môi trường và CORS.
- Chuẩn bị tích hợp persistence. `pom.xml` hiện chưa bật dependency Spring Data JPA và MySQL; chỉ bổ sung/cấu hình theo quyết định kiến trúc của dự án.
- Chuẩn bị cấu hình bảo mật theo từng giai đoạn; không đưa secret vào mã nguồn.
- Chốt cách frontend React + Vite hiện tại gọi API và quản lý phiên đăng nhập. Không chuyển framework chỉ vì tài liệu đề xuất Next.js.

**Đầu ra/điều kiện hoàn tất:** cấu trúc package thống nhất, ứng dụng khởi chạy được, cấu hình môi trường tách biệt và một API mẫu trả DTO qua luồng Controller → Service → Repository.

## P1 — Tài khoản, xác thực và vai trò

**Package:** `auth`, `user`  
**Use Case:** UC01–UC03, UC07

**Nghiệp vụ:** đăng ký giáo viên/học sinh, xác minh OTP hoặc OAuth khi tích hợp được cấu hình, đăng nhập, xem/cập nhật hồ sơ, đổi mật khẩu; Admin quản lý tài khoản, vai trò và trạng thái.

**Model chính:** `User`, `Teacher`, `Student`, `Role`, `UserStatus`. Thống nhất cách biểu diễn `Teacher`/`Student` với `User` trước khi tạo schema.

**Các tầng trong feature:**

- `auth`: `AuthController`, `AuthService`, DTO xác thực; tích hợp OTP/email/OAuth qua adapter phù hợp.
- `user`: `UserController`, `UserService`, `TeacherService`, `StudentService`; các Repository và Entity tương ứng.
- Phân quyền được kiểm tra ở backend. Không trả mật khẩu, OTP hoặc thông tin nhạy cảm trong response.

**Điều kiện hoàn tất:** các luồng đăng nhập/đăng ký/hồ sơ cơ bản hoạt động; endpoint quản trị chỉ dành cho Admin; người dùng bị khóa hoặc chưa xác minh được xử lý theo trạng thái đã thống nhất.

## P2 — Chương trình học

**Package:** `academic`  
**Use Case:** UC06

**Trạng thái: Hoàn tất (giao diện và API yêu cầu phiên đăng nhập ADMIN/TEACHER; phiên đăng nhập phụ thuộc P1).**

**Nghiệp vụ:** Admin/giáo viên được phân quyền quản lý môn học, chủ đề và bài học/đơn vị kiến thức theo quan hệ `Subject → Topic → Lesson`.

**Model chính:** `Subject`, `Topic`, `Lesson`.

**Các tầng trong feature:** `SubjectController`, `TopicController`, `LessonController`; service cho CRUD, chuẩn hóa tên, kiểm tra trùng và quan hệ cha-con; repository cho truy vấn; DTO/validation cho dữ liệu vào/ra. API hiện có: `/api/subjects`, `/api/topics?subjectId=…`, `/api/lessons?topicId=…` cùng các thao tác đọc, tạo, sửa, xóa theo ID.

**Điều kiện hoàn tất:** chương trình Toán lớp 2 có thể được nhập và quản lý; mỗi câu hỏi có thể tham chiếu một `Lesson` ổn định để phục vụ chấm điểm và phân tích năng lực.

## P3 — Lớp học và học sinh

**Package:** `classroom` (dùng `Student` từ `user`)  
**Use Case:** UC04–UC05

**Nghiệp vụ:** giáo viên tạo/xem/cập nhật lớp được phân quyền, thêm học sinh, tra cứu danh sách và thông tin học sinh.

**Model chính:** `Classroom`; quan hệ tới `Teacher` và `Student`. `Student` thuộc tối đa một lớp hiện tại qua `classroom_id`, phù hợp trường `classId` trong `TLCN.md`; chưa lưu lịch sử chuyển lớp vì use case hiện tại chưa yêu cầu.

**Các tầng trong feature:** `ClassroomController`, `ClassroomService`, `ClassroomRepository`, Entity/DTO lớp học. Kiểm tra quyền sở hữu lớp và quyền truy cập hồ sơ ở backend.

**API đã triển khai:** `GET/POST /api/classrooms`, `GET/PUT /api/classrooms/{id}`, `GET /api/classrooms/{id}/students`, `POST/DELETE /api/classrooms/{id}/students/{studentId}`, `GET /api/students/candidates?email=…`, `GET/PATCH /api/students/{studentId}`. Admin có thể lấy danh sách giáo viên qua `GET /api/classrooms/teachers` để giao lớp. API yêu cầu phiên đăng nhập `ADMIN` hoặc `TEACHER`; giáo viên chỉ đọc/sửa lớp mình phụ trách và học sinh thuộc các lớp đó.

**Điều kiện hoàn tất:** giáo viên chỉ thao tác trên lớp được giao; học sinh được liên kết chính xác với lớp; tìm kiếm/lọc danh sách có phạm vi quyền rõ ràng.

## P4 — Ngân hàng câu hỏi, kiểm tra và chấm điểm

**Package:** `assessment`  
**Use Case:** UC08–UC12

**Nghiệp vụ:** giáo viên tạo bài kiểm tra và lưu nháp; học sinh làm bài, lưu tạm, nộp bài; hệ thống chấm tự động và cung cấp kết quả/lịch sử.

**Model chính:** `Assessment`, `Question`, `MultipleChoiceQuestion`, `FillBlankQuestion`, `ShortAnswerQuestion`, `Option`, `Blank`, `AssessmentAttempt`, `StudentAnswer`, `MultipleChoiceAnswer`, `FillBlankAnswer`, `ShortAnswerAnswer`, `StudentBlankAnswer`.

**Các tầng trong feature:** `AssessmentController`, `AssessmentAttemptController`, `AssessmentResultController`; `AssessmentService`, `AssessmentAttemptService`, `GradingService`, `AssessmentResultService`; repository cho aggregate/lưu trữ tương ứng; DTO riêng cho đề thi, bài nộp và kết quả.

**Điều kiện hoàn tất:** câu hỏi gắn với `Lesson`; cả ba dạng câu hỏi được chấm theo quy tắc xác định; nộp bài và chấm điểm có giao dịch nhất quán; đáp án không bị lộ trước thời điểm cho phép; học sinh và giáo viên chỉ xem dữ liệu phù hợp quyền.

## P5 — Phân tích năng lực và khoảng trống kiến thức

**Package:** `mastery`, `ai`  
**Use Case:** UC13–UC19

**Nghiệp vụ:** phân tích bài làm theo câu hỏi/bài học, cập nhật mức thành thạo, phân loại lỗi, xác định khoảng trống kiến thức và cung cấp hồ sơ năng lực/dashboard lớp cho giáo viên.

**Model chính trong `mastery`:** `KnowledgeMastery`, `MasteryHistory`, `StudentError`, `KnowledgeGap`.

**Các tầng trong feature:**

- `mastery`: repository, service và controller cho hồ sơ năng lực, lịch sử mastery, dạng lỗi, knowledge gap và dashboard.
- `ai`: interface/client kết nối nhà cung cấp và DTO kết quả; không sở hữu bản sao entity nghiệp vụ.
- Giữ phép tính điểm/độ tin cậy theo quy tắc xác định; dùng AI cho phân loại/nhận xét khi phù hợp. Lưu tham chiếu tới câu trả lời và `Lesson` làm nguồn phân tích.

**Điều kiện hoàn tất:** kết quả phân tích truy vết được về dữ liệu bài làm; giáo viên xem được hồ sơ từng học sinh, lịch sử thay đổi và thống kê lớp; lỗi tích hợp AI không làm mất bài nộp hoặc kết quả chấm.

## P6 — Lộ trình học tập cá nhân hóa

**Package:** `learningpath`  
**Use Case:** UC20–UC25

**Nghiệp vụ:** tạo lộ trình từ `KnowledgeGap`, tạo và sắp xếp bước học theo ưu tiên/tiên quyết, cho giáo viên và học sinh xem mục tiêu, trạng thái và tiến độ.

**Model chính:** `LearningPath`, `LearningStep`; tham chiếu `Student`, `Lesson`, `KnowledgeGap`.

**Các tầng trong feature:** `LearningPathController`, `LearningStepController` (hoặc controller gộp theo hợp đồng API), `LearningPathService`, `LearningStepService`, `LearningProgressService`, repository và DTO tương ứng.

**Điều kiện hoàn tất:** mỗi bước gắn với kiến thức cụ thể; thứ tự và trạng thái bước hợp lệ; tiến độ tính nhất quán; quyền xem của học sinh chỉ giới hạn lộ trình của chính mình.

## P7 — Bài tập cá nhân hóa và giáo viên duyệt

**Package:** `exercise`, tích hợp `assessment`, `learningpath`, `ai`  
**Use Case:** UC26–UC31

**Nghiệp vụ:** sinh câu hỏi theo mục tiêu của `LearningStep`, lưu chờ duyệt, giáo viên xem/chỉnh sửa/phê duyệt, học sinh làm bài tập đã duyệt và xem kết quả.

**Model:** tái sử dụng `Question` và các subtype thuộc `assessment`; liên kết câu hỏi với `LearningStep`. Chỉ thêm model assignment/review nếu nghiệp vụ cần trạng thái hoặc lịch sử riêng.

**Các tầng trong feature:** controller cho danh sách bài tập và thao tác duyệt; `ExerciseGenerationService`, `ExerciseReviewService`, `ExerciseAssignmentService`; repository chỉ cho model riêng của exercise; DTO phù hợp từng vai trò.

**Điều kiện hoàn tất:** bài tập AI luôn bắt đầu ở `PENDING`; chỉ giáo viên có quyền mới duyệt/chỉnh sửa; học sinh chỉ nhận bài `APPROVED`; bài tập dùng lại luồng làm bài/chấm điểm của `assessment` thay vì tạo cơ chế trả lời thứ hai.

## P8 — Đánh giá lại và khép kín vòng học tập

**Package:** `mastery`, `learningpath`, `ai`, `exercise`  
**Use Case:** UC32–UC35

**Nghiệp vụ:** tiếp nhận bài làm mới, cập nhật mastery và confidence, so sánh với mục tiêu, giữ nguyên hoặc điều chỉnh lộ trình và cập nhật tiến độ sau khi luyện tập.

**Luồng chính:** bài tập nộp thành công → chấm điểm → ghi nhận nguồn kết quả → cập nhật `KnowledgeMastery`/`MasteryHistory` và `confidenceScore` → cập nhật `KnowledgeGap` → giữ hoặc điều chỉnh `LearningPath`/`LearningStep`.

**Điều kiện hoàn tất:** mỗi kết quả chỉ được xử lý một lần hoặc có cơ chế idempotency; lỗi ở phân tích/điều chỉnh có thể thử lại mà không nhân đôi lịch sử hoặc bước học; trạng thái cập nhật được lưu và có thể kiểm tra.

## P9 — Hoàn thiện và nghiệm thu

**Phạm vi:** xuyên suốt các package; tương ứng giai đoạn 5 trong `TLCN.md`.

- Chuẩn bị dữ liệu mẫu chương trình Toán lớp 2 và kịch bản học sinh có mức độ thành thạo khác nhau.
- Hoàn thiện giao diện theo vai trò, đặc biệt luồng làm bài và lộ trình phù hợp học sinh tiểu học.
- Rà soát quyền API, bảo vệ dữ liệu học sinh, hiệu năng truy vấn và cấu hình triển khai.
- Hoàn thiện thông báo thời gian thực và Docker khi các luồng cốt lõi đã ổn định.
- Nghiệm thu vòng khép kín từ bài kiểm tra ban đầu đến phân tích, lộ trình, bài luyện tập được duyệt và đánh giá lại.

## Thứ tự triển khai bên trong một feature

1. Chốt nghiệp vụ, trạng thái, quyền truy cập và tiêu chí nghiệm thu.
2. Xác định model/quan hệ và migration database.
3. Viết Repository cho các truy vấn mà Service cần.
4. Viết Service chứa quy tắc nghiệp vụ, giao dịch và kiểm tra quyền theo phạm vi dữ liệu.
5. Tạo DTO/validation và Controller để công bố API.
6. Tích hợp frontend theo vai trò và trạng thái tải/lỗi/thành công.
7. Thực hiện kiểm tra/xác minh theo yêu cầu của task hoặc kế hoạch nhóm.

## Theo dõi tiến độ

Đánh dấu mỗi feature bằng một trạng thái: `Chưa bắt đầu`, `Đang thực hiện`, `Bị chặn`, `Hoàn tất`. Chỉ đánh dấu hoàn tất khi tiêu chí nghiệm thu của feature đạt và các Use Case được liệt kê đã có luồng sử dụng được. Khi thay đổi thứ tự hoặc phạm vi, cập nhật lý do và các phụ thuộc bị ảnh hưởng trong tài liệu này.
