# KẾ HOẠCH XÂY DỰNG NỀN TẢNG HỌC TẬP SỐ CÁ NHÂN HÓA (TIỂU HỌC)

**Đề tài:** Xây dựng nền tảng học tập số hỗ trợ giáo viên đánh giá năng lực và đề xuất nội dung học tập cá nhân hóa cho học sinh tiểu học (Trọng tâm: Toán lớp 2).

**Sinh viên thực hiện:** Dương Đăng Khoa (23110240) - Hồ Minh Trí (23110349)

**Giảng viên hướng dẫn:** TS. Mai Anh Thơ

## PHẦN 1: TỔNG QUAN KIẾN TRỨC & CÔNG NGHỆ

### 1. Kiến trúc hệ thống

* **Mô hình giao tiếp:** Client – Server kết hợp thiết kế **API-First** (RESTful API & WebSocket).

* **Kiến trúc Backend:** **3-Layer Architecture** (Presentation Tier / Controller $\rightarrow$ Business Tier / Service & DTO $\rightarrow$ Data Tier / Repository).

* **Luồng nghiệp vụ cốt lõi (Core Loop):**
  `Assessment (Kiểm tra)` $\rightarrow$ `Learning Data (Dữ liệu làm bài)` $\rightarrow$ `Mastery & Error Analysis (Phân tích năng lực & lỗi)` $\rightarrow$ `Knowledge Gap (Khoảng trống kiến thức)` $\rightarrow$ `Personalized Learning Path (Lộ trình cá nhân hóa)` $\rightarrow$ `AI-generated Exercises (Sinh bài tập AI & Giáo viên duyệt)` $\rightarrow$ `Student Practice (Học sinh luyện tập)` $\rightarrow$ `Re-assessment (Đánh giá lại & Cập nhật hồ sơ)`.

### 2. Công nghệ sử dụng (Tech Stack)

* **Frontend:** ReactJS (Next.js), TypeScript, Tailwind CSS.

* **Backend:** Java, Spring Boot (Spring Security, Spring Data JPA).

* **Cơ sở dữ liệu & Lưu trữ:** MySQL (RDBMS), Cloudinary (Lưu trữ media/ảnh đại diện/hình ảnh câu hỏi).

* **Tích hợp & Triển khai:** AI Service (LLM/Generative AI API cho phân tích và sinh bài tập), Socket.IO (Thông báo thời gian thực), Docker.

## PHẦN 2: DANH SÁCH NGHIỆP VỤ CHI TIẾT (35 USE CASES)

Hệ thống bao gồm 4 tác nhân chính: **Admin (Quản trị viên)**, **Teacher (Giáo viên)**, **Student (Học sinh)**, và **AI Service (Dịch vụ AI)**.

### Nhóm 1: Quản lý tài khoản, Phân quyền & Dữ liệu học thuật

1. **UC01 – Đăng ký:** Giáo viên/Học sinh đăng ký tài khoản bằng Email (xác thực OTP) hoặc Google OAuth2.

2. **UC02 – Đăng nhập:** Đăng nhập vào hệ thống (hỗ trợ JWT Token, Ghi nhớ đăng nhập, Google Login).

3. **UC03 – Quản lý hồ sơ cá nhân:** Xem, cập nhật thông tin cơ bản, đổi ảnh đại diện (Cloudinary), đổi mật khẩu.

4. **UC04 – Quản lý lớp học (Teacher):** Tạo mới, xem danh sách, xem chi tiết, cập nhật thông tin lớp học được phân quyền.

5. **UC05 – Quản lý học sinh (Teacher):** Thêm học sinh vào lớp, xem danh sách/chi tiết, lọc/tìm kiếm và cập nhật thông tin học sinh.

6. **UC06 – Quản lý môn học, chủ đề và kiến thức (Admin, Teacher):** CRUD cấu trúc chương trình học: Môn học (`Subject`) $\rightarrow$ Chủ đề (`Topic`) $\rightarrow$ Bài học/Đơn vị kiến thức (`Lesson`).

7. **UC07 – Quản lý tài khoản và phân quyền (Admin):** Xem danh sách user, tạo tài khoản, phân quyền (`Role`: Admin, Teacher, Student), khóa/mở khóa tài khoản.

### Nhóm 2: Tổ chức Kiểm tra & Đánh giá (Assessment)

 8. **UC08 – Tạo và quản lý bài kiểm tra (Teacher):** Thiết lập thông tin bài kiểm tra, thời gian, gắn các câu hỏi (Trắc nghiệm, Điền khuyết, Trả lời ngắn) liên kết với từng đơn vị kiến thức (`Lesson`). Hỗ trợ lưu nháp.

 9. **UC09 – Thực hiện bài kiểm tra (Student):** Xem danh sách bài kiểm tra, làm bài trong thời gian quy định, hỗ trợ lưu tạm câu trả lời.

10. **UC10 – Nộp bài kiểm tra (Student):** Xác nhận nộp bài, ghi nhận thời gian nộp và tự động kích hoạt luồng chấm điểm.

11. **UC11 – Xem kết quả kiểm tra (Student, Teacher):** Xem điểm số, chi tiết đúng/sai của từng câu hỏi sau khi hoàn thành.

12. **UC12 – Xem lịch sử kết quả học tập (Student, Teacher):** Tra cứu lịch sử các lần làm bài kiểm tra và bài tập theo thời gian/môn học.

### Nhóm 3: Phân tích năng lực & Hồ sơ học sinh (AI Service & Teacher)

13. **UC13 – Phân tích kết quả học tập (AI Service):** Phân tích dữ liệu bài làm theo từng câu hỏi và đơn vị kiến thức liên kết.

14. **UC14 – Đánh giá mức độ thành thạo (AI Service):** Tính toán hoặc suy luận mức độ thành thạo (`KnowledgeMastery`) của học sinh trên từng đơn vị kiến thức.

15. **UC15 – Phân tích dạng lỗi (AI Service):** Nhận diện và phân loại các lỗi sai thường gặp (`StudentError`) từ câu trả lời sai của học sinh.

16. **UC16 – Xem hồ sơ năng lực của học sinh (Teacher):** Xem tổng hợp mức độ thành thạo, các dạng lỗi hay mắc và khoảng trống kiến thức của từng học sinh.

17. **UC17 – Xem dashboard tổng quan năng lực lớp (Teacher):** Xem biểu đồ thống kê năng lực cả lớp, nhận diện các kiến thức nhiều học sinh đang yếu.

18. **UC18 – Theo dõi mức độ thành thạo theo kiến thức (Teacher):** Xem chi tiết điểm thành thạo hiện tại và lịch sử biến đổi năng lực của học sinh trên từng kiến thức cụ thể.

19. **UC19 – Xác định khoảng trống kiến thức (AI Service):** Đối chiếu điểm thành thạo và tần suất lỗi với ngưỡng chuẩn để tìm ra `KnowledgeGap`, nguyên nhân và mức độ ưu tiên cần ôn tập.

### Nhóm 4: Đề xuất & Quản lý Lộ trình học tập (Learning Path)

20. **UC20 – Đề xuất lộ trình học cá nhân hóa (AI Service):** Tự động tạo lộ trình học tập (`LearningPath`) dựa trên hồ sơ năng lực và khoảng trống kiến thức của học sinh.

21. **UC21 – Tạo các bước học tập (AI Service):** Sinh các bước học tập cụ thể (`LearningStep`) gồm mục tiêu, nội dung và điểm mục tiêu cho lộ trình.

22. **UC22 – Sắp xếp các bước học tập theo lộ trình (AI Service):** Sắp xếp thứ tự (`order`) các bước học tập theo điều kiện tiên quyết và mức độ ưu tiên.

23. **UC23 – Xem lộ trình học tập (Teacher, Student):** Xem tổng quan lộ trình cá nhân hóa và mục tiêu học tập.

24. **UC24 – Xem các bước học tập (Student - Extend UC23):** Xem chi tiết từng bước học tập, trạng thái hoàn thành và nội dung cần làm trong bước đó.

25. **UC25 – Theo dõi tiến độ hoàn thành lộ trình (Teacher, Student):** Theo dõi số bước đã hoàn thành, đang học, chưa học và tỷ lệ % hoàn thành lộ trình.

### Nhóm 5: Sinh bài tập cá nhân hóa & Đánh giá lại (Vòng lặp khép kín)

26. **UC26 – Sinh bài tập (AI Service):** Dùng Generative AI tự động tạo câu hỏi/bài tập kèm đáp án dựa trên kiến thức mục tiêu của `LearningStep`, lưu ở trạng thái **Chờ duyệt**.

27. **UC27 – Xem bài tập do AI sinh (Teacher, Student):** Giáo viên xem toàn bộ câu hỏi/đáp án chờ duyệt; Học sinh xem các bài tập đã được giáo viên phê duyệt.

28. **UC28 – Phê duyệt/chỉnh sửa bài tập (Teacher):** Giáo viên kiểm tra, chỉnh sửa nội dung/đáp án câu hỏi do AI sinh và bấm Phê duyệt để giao cho học sinh.

29. **UC29 – Thực hiện bài tập (Student):** Học sinh mở bài tập ôn tập trong từng bước học tập để làm bài.

30. **UC30 – Nộp câu trả lời bài tập (Student - Include UC29):** Gửi bài làm ôn tập lên hệ thống để chấm điểm.

31. **UC31 – Xem kết quả bài tập (Student):** Xem điểm số, đáp án đúng/sai và nhận xét sau khi nộp bài tập ôn luyện.

32. **UC32 – Tiếp nhận kết quả làm bài mới (AI Service):** Thu thập kết quả bài tập vừa làm để chuẩn bị cho chu kỳ đánh giá lại.

33. **UC33 – Đánh giá và điều chỉnh lộ trình mới (AI Service):** So sánh kết quả mới với mục tiêu lộ trình để giữ nguyên hoặc tự động điều chỉnh/bổ sung các bước học tập.

34. **UC34 – Cập nhật mức độ thành thạo (AI Service):** Tính toán lại điểm thành thạo (`Mastery Score`) sau khi học sinh hoàn thành bài luyện tập.

35. **UC35 – Cập nhật độ tin cậy trong hồ sơ năng lực (AI Service):** Cập nhật chỉ số độ tin cậy (`Confidence Level`) của đánh giá năng lực dựa trên số lượng và tính nhất quán của dữ liệu làm bài mới.

## PHẦN 3: THIẾT KẾ MODEL (DATABASE ENTITIES)

Dựa trên **Lược đồ lớp (Mục 3.3)** và **Yêu cầu chức năng phân tích AI (Mục 1.1 & 1.2)** trong báo cáo, danh sách các Model trong hệ thống được chia thành 2 phần: **Các Model đã có trong Lược đồ lớp** và **Các Model cần bổ sung** để đáp ứng trọn vẹn 35 Use Cases.

### A. Các Model cốt lõi (Đã định nghĩa trong Lược đồ lớp 3.3)

#### 1. Phân hệ Người dùng & Lớp học

* **`User`** (Lớp cha/Bảng người dùng chung)

  * `userId`: Int (PK)

  * `fullName`: String

  * `email`: String (Unique)

  * `password`: String

  * *Thuộc tính bổ sung từ UC01, UC03, UC07:* `phone`: String, `avatarUrl`: String, `address`: String, `city`: String, `role`: Enum (`ADMIN`, `TEACHER`, `STUDENT`), `status`: Enum (`ACTIVE`, `LOCKED`, `PENDING_OTP`), `otpCode`: String, `otpExpiredAt`: DateTime (Lưu trữ trực tiếp mã OTP xác thực trên DB).

* **`Teacher`** (Kế thừa/Liên kết `User`)

  * Quản lý các `Classroom` và tạo các `Assessment`.

* **`Student`** (Kế thừa/Liên kết `User`)

  * `sex`: String

  * `YOB`: DateTime (Ngày tháng năm sinh)

  * `classId`: Int (FK liên kết `Classroom`)

* **`Classroom`** (Lớp học)

  * `classId`: Int (PK)

  * `name`: String (VD: "Lớp 2A1")

  * `schoolYear`: String (VD: "2025-2026")

  * `teacherId`: Int (FK $\rightarrow$ `Teacher`)

  * `students`: List<Student>

#### 2. Phân hệ Dữ liệu Học thuật (Chương trình Toán Lớp 2)

* **`Subject`** (Môn học)

  * `subjectId`: Int (PK)

  * `name`: String (VD: "Toán 2")

  * `topics`: List<Topic>

* **`Topic`** (Chủ đề)

  * `topicId`: Int (PK)

  * `name`: String (VD: "Phép cộng, phép trừ trong phạm vi 20")

  * `subjectId`: Int (FK $\rightarrow$ `Subject`)

  * `lessons`: List<Lesson>

* **`Lesson`** (Bài học / Đơn vị kiến thức chuẩn)

  * `lessonId`: Int (PK)

  * `name`: String (VD: "Phép cộng có nhớ trong phạm vi 20")

  * `description`: String

  * `topicId`: Int (FK $\rightarrow$ `Topic`)

#### 3. Phân hệ Câu hỏi & Bài kiểm tra (Question Bank & Assessment)

* **`Assessment`** (Bài kiểm tra)

  * `assessmentId`: Int (PK)

  * `title`: String

  * `type`: String (VD: `DIAGNOSTIC`, `PERIODIC`, `PRACTICE`)

  * `duration`: Int (Thời gian làm bài - phút)

  * `createdAt`: DateTime

  * `teacherId`: Int (FK $\rightarrow$ `Teacher`)

  * `questions`: List<Question>

* **`Question`** (Lớp cha của các loại câu hỏi - Dùng chung cho cả Kiểm tra và Bài tập AI sinh)

  * `questionId`: Int (PK)

  * `content`: String (Nội dung đề bài)

  * `difficulty`: String (`EASY`, `MEDIUM`, `HARD`)

  * `score`: Double

  * `lessonId`: Int (FK $\rightarrow$ `Lesson` - Gắn nhãn đơn vị kiến thức)

  * *Thuộc tính bổ sung cho UC26, UC28:* `isAiGenerated`: Boolean, `approvalStatus`: Enum (`PENDING`, `APPROVED`, `REJECTED`), `explanation`: Text (Lời giải chi tiết).

* **`MultipleChoiceQuestion`** (Kế thừa `Question` - Câu hỏi trắc nghiệm)

  * `allowMultiple`: Boolean

  * `options`: List<Option>

* **`Option`** (Lựa chọn trắc nghiệm)

  * `optionId`: Int (PK)

  * `name`: String (Nội dung lựa chọn)

  * `isCorrect`: Boolean

  * `displayOrder`: Int

  * `questionId`: Int (FK $\rightarrow$ `MultipleChoiceQuestion`)

* **`FillBlankQuestion`** (Kế thừa `Question` - Câu hỏi điền khuyết)

  * `caseSensitive`: Boolean

  * `blanks`: List<Blank>

* **`Blank`** (Ô trống cần điền)

  * `blankId`: Int (PK)

  * `correctAnswer`: String

  * `position`: Int (Vị trí ô trống)

  * `questionId`: Int (FK $\rightarrow$ `FillBlankQuestion`)

* **`ShortAnswerQuestion`** (Kế thừa `Question` - Câu hỏi trả lời ngắn)

  * `caseSensitive`: Boolean

  * *Ghi chú:* Cần thêm trường `correctAnswer`: String để hệ thống tự động chấm.

#### 4. Phân hệ Lịch sử Làm bài & Trả lời của Học sinh

* **`AssessmentAttempt`** (Lượt làm bài kiểm tra/bài tập)

  * `attemptId`: Int (PK)

  * `startedAt`: DateTime

  * `submittedAt`: DateTime

  * `status`: Enum (`IN_PROGRESS`, `SUBMITTED`) (Phục vụ lưu tạm bài làm khi chưa nộp)

  * `assessmentId`: Int (FK $\rightarrow$ `Assessment`, nullable nếu làm bài tập theo `LearningStep`)

  * `studentId`: Int (FK $\rightarrow$ `Student`)

  * `answers`: List<StudentAnswer>

  * *Phương thức:* `+Double totalScore()`

* **`StudentAnswer`** (Lớp cha câu trả lời của học sinh)

  * `answerId`: Int (PK)

  * `isCorrect`: Boolean

  * `commentOfTeacher`: String

  * `submittedAt`: DateTime

  * `questionId`: Int (FK $\rightarrow$ `Question`)

  * `attemptId`: Int (FK $\rightarrow$ `AssessmentAttempt`)

  * *Phương thức:* `+Double score()`

* **`MultipleChoiceAnswer`** *(Trong sơ đồ lớp ghi trùng tên là `FillBlankAnswer` liên kết với `Option`, khi code cần đổi tên thành `MultipleChoiceAnswer`)*

  * `answers`: List<Option> (Các lựa chọn học sinh đã chọn)

* **`FillBlankAnswer`** (Kế thừa `StudentAnswer`)

  * `answers`: List<StudentBlankAnswer>

* **`StudentBlankAnswer`** (Chi tiết từng ô trống học sinh điền)

  * `studentblankAnswerId`: Int (PK)

  * `content`: String

  * `blankId`: Int (FK $\rightarrow$ `Blank`)

* **`ShortAnswerAnswer`** (Kế thừa `StudentAnswer`)

  * `content`: String

#### 5. Phân hệ Lộ trình Học tập Cá nhân hóa

* **`LearningPath`** (Lộ trình học tập)

  * `pathId`: Int (PK)

  * `status`: String (`ACTIVE`, `COMPLETED`, `ARCHIVED`)

  * `createdAt`: DateTime

  * `updatedAt`: DateTime

  * `studentId`: Int (FK $\rightarrow$ `Student`)

  * `steps`: List<LearningStep>

* **`LearningStep`** (Bước học tập trong lộ trình)

  * `stepId`: Int (PK)

  * `order`: Int (Thứ tự bước học)

  * `status`: String (`LOCKED`, `IN_PROGRESS`, `COMPLETED`)

  * `targetScore`: Double (Điểm mục tiêu cần đạt)

  * `pathId`: Int (FK $\rightarrow$ `LearningPath`)

  * `lessonId`: Int (FK $\rightarrow$ `Lesson` - Kiến thức cần ôn ở bước này)

  * `exercises`: List<Question> (Danh sách câu hỏi bài tập do AI sinh gắn với bước này)

### B. Các Model Bổ Sung (Cần thiết cho AI Service & Hồ sơ năng lực)

Trong Mục 1.1, 1.2 và các Use Case từ UC13 $\rightarrow$ UC20, UC34, UC35 có nêu rõ hệ thống cần lưu trữ `KnowledgeMastery`, `StudentError`, `KnowledgeGap` và lịch sử biến đổi năng lực, nhưng sơ đồ lớp ở mục 3.3 chưa vẽ các bảng này. Nhóm cần bổ sung 4 Model sau vào Database:

1. **`KnowledgeMastery`** (Mức độ thành thạo kiến thức - Phục vụ UC14, UC16, UC18, UC34, UC35)

   * `masteryId`: Int (PK)

   * `studentId`: Int (FK $\rightarrow$ `Student`)

   * `lessonId`: Int (FK $\rightarrow$ `Lesson`)

   * `masteryScore`: Double (Điểm thành thạo, VD: thang $0.0 \rightarrow 1.0$ hoặc $0 \rightarrow 100$)

   * `masteryLevel`: Enum (`NOT_STARTED`, `BEGINNING`, `DEVELOPING`, `PROFICIENT`, `MASTERED`)

   * `confidenceScore`: Double (Độ tin cậy của đánh giá - Phục vụ UC35)

   * `lastUpdated`: DateTime

2. **`MasteryHistory`** (Lịch sử thay đổi độ thành thạo - Phục vụ UC18)

   * `historyId`: Int (PK)

   * `masteryId`: Int (FK $\rightarrow$ `KnowledgeMastery`)

   * `oldScore`: Double

   * `newScore`: Double

   * `recordedAt`: DateTime

   * `triggerSource`: String (VD: `ASSESSMENT_10`, `LEARNING_STEP_5`)

3. **`StudentError`** (Phân tích dạng lỗi của học sinh - Phục vụ UC15, UC16, UC17)

   * `errorId`: Int (PK)

   * `studentId`: Int (FK $\rightarrow$ `Student`)

   * `lessonId`: Int (FK $\rightarrow$ `Lesson`)

   * `answerId`: Int (FK $\rightarrow$ `StudentAnswer` - Nguồn câu trả lời gây ra lỗi)

   * `errorType`: String (Phân loại lỗi, VD: "Quên nhớ khi cộng qua 10", "Nhầm lẫn dấu cộng và trừ", "Đọc sai đề bài toán có lời văn")

   * `description`: Text (Nhận xét chi tiết từ AI)

   * `createdAt`: DateTime

4. **`KnowledgeGap`** (Khoảng trống kiến thức - Phục vụ UC16, UC17, UC19, UC20)

   * `gapId`: Int (PK)

   * `studentId`: Int (FK $\rightarrow$ `Student`)

   * `lessonId`: Int (FK $\rightarrow$ `Lesson`)

   * `priority`: Enum (`HIGH`, `MEDIUM`, `LOW` - Mức độ ưu tiên cần củng cố)

   * `rootCause`: Text (Nguyên nhân gốc rễ do AI phân tích từ `KnowledgeMastery` và `StudentError`)

   * `status`: Enum (`IDENTIFIED`, `IN_REMEDIATION`, `RESOLVED`)

   * `identifiedAt`: DateTime

## PHẦN 4: LỘ TRÌNH XÂY DỰNG WEB (IMPLEMENTATION PLAN)

### Giai đoạn 1: Khởi tạo dự án & Xây dựng Core Hệ thống (Tuần 1 - 2)

* **Mục tiêu:** Thiết lập môi trường, cấu trúc 3-Layer Backend, Frontend boilerplate và hoàn thành Nhóm nghiệp vụ 1 (UC01 $\rightarrow$ UC07).

* **Công việc Backend (Spring Boot):**

  * Thiết lập Docker Compose cho MySQL.

  * Cấu hình Spring Security + JWT + OAuth2 (Google) + Gửi mail OTP (lưu mã OTP và thời gian hết hạn trực tiếp trong bảng `User`).

  * Xây dựng các Entity, Repository, Service, Controller cho `User`, `Teacher`, `Student`, `Classroom`, `Subject`, `Topic`, `Lesson`.

  * Tích hợp Cloudinary API để upload ảnh đại diện.

* **Công việc Frontend (Next.js + Tailwind):**

  * Thiết lập cấu trúc thư mục, Axios interceptor (quản lý token), phân quyền Route (Middleware).

  * Xây dựng giao diện Auth (Đăng nhập, Đăng ký, Nhập OTP), Trang cá nhân.

  * Xây dựng giao diện Admin (Quản lý User/Phân quyền, Quản lý chương trình học Toán lớp 2) và giao diện Giáo viên (Quản lý Lớp học, Học sinh).

### Giai đoạn 2: Phân hệ Kiểm tra & Chấm điểm tự động (Tuần 3 - 4)

* **Mục tiêu:** Hoàn thành Nhóm nghiệp vụ 2 (UC08 $\rightarrow$ UC12).

* **Công việc Backend:**

  * Thiết kế cấu trúc kế thừa JPA (`@Inheritance`) cho `Question` (`MultipleChoice`, `FillBlank`, `ShortAnswer`) và `StudentAnswer`.

  * Xây dựng API tạo đề kiểm tra, gắn câu hỏi với `Lesson`.

  * Xây dựng API làm bài, lưu nháp câu trả lời trực tiếp vào MySQL (với trạng thái `AssessmentAttempt` là `IN_PROGRESS`) hoặc kết hợp `localStorage` ở phía Client, và nộp bài (`SUBMITTED`).

  * Xây dựng thuật toán chấm điểm tự động cho 3 loại câu hỏi ngay khi học sinh nộp bài.

* **Công việc Frontend:**

  * Giao diện Giáo viên: Trình tạo bài kiểm tra (Question Builder hỗ trợ tạo trắc nghiệm, điền khuyết, trả lời ngắn).

  * Giao diện Học sinh (Thiết kế thân thiện, màu sắc trực quan cho học sinh lớp 2): Màn hình làm bài kiểm tra có đồng hồ đếm ngược, chuyển câu hỏi mượt mà, xem kết quả và lịch sử làm bài.

### Giai đoạn 3: Tích hợp AI Service - Phân tích Năng lực & Hồ sơ Học sinh (Tuần 5 - 6)

* **Mục tiêu:** Hoàn thành Nhóm nghiệp vụ 3 (UC13 $\rightarrow$ UC19).

* **Công việc Backend & AI Integration:**

  * Xây dựng module `AIServiceClient` trong Spring Boot (gọi API LLM như OpenAI/Gemini hoặc Microservice AI riêng).

  * Thiết kế Prompt Engineering / Rule-based kết hợp AI để:

    * Tính toán `KnowledgeMastery` và `confidenceScore` dựa trên tỷ lệ đúng/sai, độ khó câu hỏi và lịch sử làm bài.

    * Phân tích câu trả lời sai của học sinh để trích xuất `StudentError` (dạng lỗi) và xác định `KnowledgeGap` (khoảng trống kiến thức + mức độ ưu tiên).

  * Xây dựng các API thống kê dữ liệu cho Dashboard của lớp và Hồ sơ năng lực cá nhân.

* **Công việc Frontend:**

  * Giao diện Giáo viên: Dashboard tổng quan lớp (Biểu đồ phân bố mức độ thành thạo, danh sách kiến thức báo động đỏ), Trang chi tiết Hồ sơ năng lực của từng học sinh (Biểu đồ lịch sử tiến bộ, danh sách lỗi thường gặp).

### Giai đoạn 4: Cá nhân hóa Lộ trình & Sinh bài tập bằng Generative AI (Tuần 7 - 8)

* **Mục tiêu:** Hoàn thành Nhóm nghiệp vụ 4 & 5 (UC20 $\rightarrow$ UC35) – Khép kín vòng lặp học tập.

* **Công việc Backend & AI Integration:**

  * Xây dựng luồng tự động đề xuất `LearningPath` và sinh các `LearningStep` được sắp xếp theo thứ tự ưu tiên từ `KnowledgeGap`.

  * Xây dựng chức năng Generative AI sinh câu hỏi bài tập (`Question` với `isAiGenerated = true`, `approvalStatus = PENDING`) bám sát nội dung Toán lớp 2 của từng `LearningStep`.

  * Xây dựng API để Giáo viên xem, chỉnh sửa và phê duyệt (`APPROVED`) bài tập AI sinh.

  * Xây dựng luồng Re-assessment (Đánh giá lại): Khi học sinh nộp bài tập trong `LearningStep`, kích hoạt AI Service cập nhật lại `KnowledgeMastery`, `confidenceScore` và điều chỉnh trạng thái `LearningPath`.

  * Tích hợp **Socket.IO** để thông báo thời gian thực cho Giáo viên (khi AI phân tích xong / sinh xong bài tập chờ duyệt) và cho Học sinh (khi có lộ trình hoặc bài tập mới được duyệt).

* **Công việc Frontend:**

  * Giao diện Giáo viên: Màn hình duyệt và chỉnh sửa bài tập do AI sinh, màn hình theo dõi tiến độ lộ trình của học sinh.

  * Giao diện Học sinh: Bản đồ Lộ trình học tập cá nhân hóa (dạng bậc thang/trò chơi hóa phù hợp học sinh tiểu học), màn hình làm bài tập ôn luyện theo từng bước.

### Giai đoạn 5: Kiểm thử thực nghiệm, Tối ưu hóa & Hoàn thiện báo cáo (Tuần 9 - 10)

* **Mục tiêu:** Đảm bảo các yêu cầu phi chức năng và chuẩn bị dữ liệu thực nghiệm Toán lớp 2.

* **Công việc:**

  * Nhập bộ dữ liệu mẫu chuẩn của chương trình Toán lớp 2 (Các chủ đề, bài học, ngân hàng câu hỏi ban đầu).

  * Chạy thử nghiệm toàn bộ quy trình khép kín với các kịch bản dữ liệu học sinh khác nhau (Học sinh giỏi, học sinh hổng kiến thức cơ bản, học sinh hay tính nhầm).

  * Kiểm thử hiệu năng, bảo mật phân quyền API và tối ưu hóa các câu truy vấn MySQL (Indexing).

  * Đóng gói ứng dụng bằng Docker và hoàn thiện cuốn báo cáo Tiểu luận chuyên ngành.