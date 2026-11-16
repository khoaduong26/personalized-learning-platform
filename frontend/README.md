## 2. Frontend TLCN

Frontend của nền tảng học tập cá nhân hóa môn Toán lớp 2, dùng React, Vite, JavaScript, Tailwind CSS và React Router.

### 2.1. Chạy và kiểm tra

Từ thư mục gốc của dự án:

```powershell
cd frontend
npm.cmd install
npm.cmd run dev -- --port 5173 --strictPort
```

Backend mặc định chạy tại `http://localhost:8080`. `vite.config.js` chuyển tiếp `/api`, `/oauth2` và `/login/oauth2` sang backend. Frontend gọi API bằng đường dẫn tương đối, không cần `.env` để chạy với cấu hình này.

```powershell
npm.cmd run lint
npm.cmd test
npm.cmd run build
npm.cmd run preview
```

`test` kiểm tra HTTP client và render các route bằng Node test runner + Vite SSR. Các kiểm tra này không thay thế kiểm thử tương tác trong trình duyệt hoặc tích hợp backend thật.

`preview` dùng để xem bản build. Cấu hình proxy hiện nằm trong `server.proxy` cho môi trường phát triển; khi triển khai phải cấu hình reverse proxy và backend phù hợp. Máy chủ hosting cần trả `index.html` cho các URL frontend như `/register`, đồng thời giữ các đường dẫn API/OAuth chuyển đến backend.

### 2.2. Kiến trúc theo chức năng

Mã được tổ chức theo ba vùng:

- **app**: khởi tạo, lắp ghép các feature, định tuyến và khung giao diện.
- **features**: nghiệp vụ; mỗi feature giữ các trang, component, API và logic liên quan gần nhau.
- **shared**: mã dùng chung giữa nhiều feature, không phụ thuộc nghiệp vụ cụ thể.

Vai trò học sinh, giáo viên, quản trị viên được phân biệt bằng route, layout và quyền truy cập. Không gom mọi nghiệp vụ của một vai trò vào các feature lớn tên `student`, `teacher`, `admin`.

### 2.3. Cấu trúc thư mục hiện có

Cây dưới đây phản ánh mã nguồn hiện tại. Các feature có `.gitkeep` là thư mục đã chuẩn bị, chưa có giao diện hoặc route; `.gitkeep` chỉ giữ thư mục trong Git.

```text
frontend/
├── public/                         # Tài nguyên truy cập trực tiếp bằng URL
│   ├── favicon.svg
│   └── icons.svg
├── src/
│   ├── main.jsx                    # Mount React, StrictMode và AppProviders
│   ├── app/
│   │   ├── App.jsx                 # Component gốc của ứng dụng
│   │   ├── providers/
│   │   │   └── AppProviders.jsx    # BrowserRouter và điểm ghép provider toàn cục
│   │   ├── routes/
│   │   │   └── AppRoutes.jsx       # Khai báo route, layout và tương thích URL cũ
│   │   ├── layouts/
│   │   │   └── PublicLayout.jsx    # Header + nội dung route + footer công khai
│   │   └── pages/
│   │       └── NotFoundPage.jsx    # Trang hệ thống khi URL không tồn tại
│   ├── features/
│   │   ├── home/
│   │   │   ├── pages/HomePage.jsx
│   │   │   ├── components/
│   │   │   │   ├── HomeHeader.jsx
│   │   │   │   ├── HomeFooter.jsx
│   │   │   │   ├── MathGarden.jsx
│   │   │   │   └── PracticePreview.jsx
│   │   │   ├── data/practice.js
│   │   │   ├── styles/home.css
│   │   │   └── index.js
│   │   ├── auth/
│   │   │   ├── pages/
│   │   │   │   ├── RegistrationPage.jsx
│   │   │   │   ├── RegistrationCompletePage.jsx
│   │   │   │   ├── GoogleRegistrationCompletePage.jsx
│   │   │   │   └── LoginPage.jsx
│   │   │   ├── components/
│   │   │   │   ├── AuthLayout.jsx
│   │   │   │   ├── RegistrationForm.jsx
│   │   │   │   ├── OtpVerificationForm.jsx
│   │   │   │   ├── OtpRecoveryForm.jsx
│   │   │   │   └── RegistrationField.jsx
│   │   │   ├── hooks/
│   │   │   │   ├── useRegistration.js
│   │   │   │   └── useOtpCountdown.js
│   │   │   ├── services/authApi.js
│   │   │   ├── utils/registrationStorage.js
│   │   │   ├── styles/registration.css
│   │   │   └── index.js
│   │   ├── users/                   # Chưa triển khai, giữ bằng .gitkeep
│   │   ├── classrooms/              # Chưa triển khai, giữ bằng .gitkeep
│   │   ├── academic/                # Chưa triển khai, giữ bằng .gitkeep
│   │   ├── assessments/             # Chưa triển khai, giữ bằng .gitkeep
│   │   ├── mastery/                 # Chưa triển khai, giữ bằng .gitkeep
│   │   ├── learning-paths/          # Chưa triển khai, giữ bằng .gitkeep
│   │   ├── exercises/               # Chưa triển khai, giữ bằng .gitkeep
│   │   └── dashboard/               # Chưa triển khai, giữ bằng .gitkeep
│   ├── shared/
│   │   ├── components/ui/Icon.jsx
│   │   ├── constants/roles.js
│   │   ├── services/httpClient.js
│   │   └── utils/formatCurrency.js
│   ├── assets/                     # Tài nguyên được import vào mã nguồn
│   ├── styles/
│   │   ├── index.css               # Điểm vào Tailwind và CSS toàn cục
│   │   └── App.css                 # CSS mẫu Vite cũ, chưa được sử dụng
│   └── prototypes/
│       └── learning/               # Bản mẫu danh mục sách cũ, không gắn route
│           ├── components/LearningResourceCard.jsx
│           ├── data/learningResources.js
│           └── pages/LearningPage.jsx
├── tests/                          # Kiểm tra HTTP client và render các route
│   ├── httpClient.test.js
│   └── routes.test.js
├── eslint.config.js
├── vite.config.js
├── index.html
├── package.json
└── package-lock.json
```

`node_modules/` chứa thư viện cài bằng npm; `dist/` là kết quả build. Không chỉnh mã trực tiếp trong hai thư mục này.

### 2.4. Trách nhiệm các thư mục

#### app: lắp ghép toàn ứng dụng

| Thư mục/file | Nhiệm vụ |
| --- | --- |
| `main.jsx` | Tìm phần tử root và khởi tạo cây React. |
| `app/App.jsx` | Component gốc; giữ việc lắp ghép ứng dụng ngắn gọn. |
| `app/providers/` | Ghép provider cần dùng toàn ứng dụng. Hiện có BrowserRouter; chỉ bổ sung provider khi có nhu cầu. |
| `app/routes/` | Ánh xạ URL tới trang, ghép layout và đặt kiểm tra đăng nhập/quyền khi triển khai. Không xử lý nghiệp vụ đăng ký hoặc chấm bài tại đây. |
| `app/layouts/` | Khung giao diện cấp ứng dụng: header, menu, footer và vùng nội dung `Outlet`. |
| `app/pages/` | Trang hệ thống không thuộc một nghiệp vụ, hiện có trang 404. Các trang nghiệp vụ phải nằm trong feature. |

`AuthLayout` nằm trong `features/auth/components` vì chỉ phục vụ các trang xác thực. `PublicLayout` nằm ở cấp app vì ghép các route công khai; header/footer được xuất từ `home`, nơi sở hữu thiết kế trang giới thiệu.

#### features: sở hữu nghiệp vụ

| Feature | Nhiệm vụ và ranh giới |
| --- | --- |
| `home` | Giới thiệu nền tảng, minh họa Toán lớp 2 và bài toán trải nghiệm không cần tài khoản. |
| `auth` | Đăng ký email, xác minh/gửi lại OTP, đăng ký và đăng nhập Google, đăng nhập email/mật khẩu, ghi nhớ đăng nhập, tải tài khoản hiện tại và đăng xuất. |
| `users` | Hồ sơ cá nhân và quản lý tài khoản, vai trò. Không quản lý trạng thái form đăng nhập. |
| `classrooms` | Lớp học và thành viên lớp. Trang xem năng lực học sinh thuộc `mastery`, dù được mở từ lớp học. |
| `academic` | Môn học, chủ đề và bài học; dữ liệu học thuật làm nền cho đánh giá và lộ trình. |
| `assessments` | Đề đánh giá, câu hỏi, lượt làm bài, câu trả lời và kết quả. Chưa cần tách mỗi loại câu hỏi hoặc lượt làm thành feature riêng. |
| `mastery` | Hồ sơ năng lực, lịch sử thay đổi, lỗi sai và kiến thức cần củng cố. |
| `learning-paths` | Lộ trình cá nhân hóa, thứ tự các bước học và tiến độ theo bước. |
| `exercises` | Bài tập theo bước học, luyện tập và quy trình giáo viên duyệt/từ chối bài tập AI đề xuất. |
| `dashboard` | Trang tổng quan theo vai trò; tổng hợp thông tin, không sao chép xử lý đánh giá hoặc tính năng lực. |

Tám feature từ `users` đến `dashboard` hiện mới có thư mục được giữ bằng `.gitkeep`; phạm vi được mô tả trong bảng trên. Không có trang giả hoặc dữ liệu mẫu được trình bày như chức năng đã hoàn thành.

#### shared: mã dùng chung

| Thư mục | Nhiệm vụ |
| --- | --- |
| `shared/components/ui/` | Thành phần giao diện tổng quát như Icon; bổ sung Button, Input, Modal khi thực sự dùng chung. |
| `shared/constants/` | Hằng số dùng giữa nhiều nghiệp vụ, hiện có `ROLES` khớp enum vai trò backend. |
| `shared/services/` | Cơ chế HTTP chung: gửi JSON, giữ AbortSignal, chuẩn hóa lỗi mạng và lỗi API. Không đặt endpoint riêng của từng nghiệp vụ ở đây. |
| `shared/utils/` | Hàm hỗ trợ dùng chung, ví dụ định dạng tiền. Logic riêng đăng ký vẫn nằm trong `auth/utils`. |

Khi phát sinh nhu cầu, có thể tạo `shared/hooks/` cho hook độc lập nghiệp vụ, hoặc `shared/components/feedback/` cho LoadingState, EmptyState, ErrorState. Không cần tạo trước file rỗng.

#### Tài nguyên, CSS và bản mẫu

- `public/`: file dùng URL cố định, ví dụ `/favicon.svg`; không import như module.
- `src/assets/`: ảnh, font hoặc tài nguyên được import trong JS/CSS và được Vite xử lý khi build.
- `src/styles/`: Tailwind và quy tắc toàn cục. CSS của trang/chức năng đặt trong `features/<feature>/styles/`, giới hạn phạm vi bằng class để tránh ảnh hưởng trang khác.
- `src/prototypes/`: mã thử nghiệm được giữ lại để tham khảo. Bản mẫu sách cũ đã chuyển từ `features/learning` sang đây; không tham gia ứng dụng chính.

### 2.5. Cách tổ chức bên trong một feature

Chỉ tạo các thư mục có mã thực tế:

```text
features/<ten-feature>/
├── pages/        # Trang gắn với route, điều phối giao diện
├── components/   # Form, bảng, thẻ, khối giao diện của nghiệp vụ
├── hooks/        # Trạng thái và luồng tương tác cần tách khỏi trang
├── services/     # API của nghiệp vụ, dùng httpClient chung
├── contexts/     # Chia sẻ trạng thái khi cần, không bắt buộc
├── utils/        # Hàm hỗ trợ riêng của feature
├── data/         # Dữ liệu tĩnh/mẫu, phải ghi rõ là dữ liệu mẫu
├── styles/       # CSS riêng
└── index.js      # Những thành phần được phép dùng từ ngoài feature
```

Trong `auth`, `RegistrationPage` chọn giao diện theo bước và hiển thị thông báo; các form nhận dữ liệu/callback qua props. `useRegistration` quản lý đăng ký, gửi lại mã, xác minh, chuyển bước và điều hướng sau thành công. `useOtpCountdown` quản lý bộ đếm và dọn timer khi unmount. `authApi` khai báo endpoint; `httpClient` chịu trách nhiệm HTTP chung.

### 2.6. Quy tắc import và đặt tên

```text
app ────────> features ────────> shared
 └────────────────────────────> shared
```

- `shared` không import `app`, `features` hoặc `prototypes`.
- `features` không import từ `app`; việc ghép các feature thuộc về app.
- `app` và các feature đang sử dụng không import `prototypes`.
- Import từ ngoài feature qua `index.js`, ví dụ `import { RegistrationPage } from '../../features/auth'`. Bên trong feature import trực tiếp file tương ứng để tránh phụ thuộc vòng qua chính `index.js`.
- Tránh phụ thuộc vòng giữa hai feature. Nếu cần dùng chung phần hiển thị câu hỏi cho kiểm tra và luyện tập, tách phần giao diện tổng quát sang shared; xử lý nộp bài vẫn do feature sở hữu.
- ESLint đang kiểm tra chiều phụ thuộc của shared, chặn feature import app và chặn app/features import prototypes. Quy tắc xuất qua `index.js` và tránh vòng giữa feature vẫn cần được kiểm tra khi review.
- Component/trang/layout dùng PascalCase; hook bắt đầu bằng `use`; service và util dùng camelCase; thư mục feature dùng chữ thường, nhiều từ nối bằng dấu `-`.
- Dùng state tại component/hook gần nơi sử dụng. Chỉ đưa state vào context khi nhiều phần thực sự cần; hiện LoginPage và HomeHeader tải người dùng qua API, chưa có AuthContext toàn cục.

### 2.7. Định tuyến và vai trò

Định tuyến dùng React Router ở chế độ khai báo: BrowserRouter, Routes, Route và Outlet. Tham khảo [tài liệu định tuyến chính thức](https://reactrouter.com/start/declarative/routing).

| URL hiện có | Hành vi |
| --- | --- |
| `/` | Trang chủ với bài toán trải nghiệm. |
| `/register` | Đăng ký, xác minh OTP hoặc gửi lại mã theo bước. |
| `/register?google=success` | Kiểm tra phiên đăng ký Google và hiển thị kết quả. |
| `/register?google=exists` hoặc `google=error` | Thông báo tương ứng trong trang đăng ký. |
| `/register/complete` | Thông báo sau khi xác minh email thành công. |
| `/login` | Đăng nhập email/mật khẩu hoặc Google, chọn ghi nhớ; khi đã đăng nhập hiển thị tài khoản, vai trò và nút đăng xuất. |
| `/login?google=success` | Tải tài khoản hiện tại sau callback Google. |
| `/login?google=not-registered`, `google=locked` hoặc `google=error` | Hiển thị lỗi đăng nhập Google tương ứng. |
| `/login?registered=1` | Tương thích luồng cũ: chuyển sang `/register/complete`. |
| URL khác | Trang 404, không tự hiển thị trang chủ. |

Trang hoàn tất chỉ là giao diện phản hồi, không phải bằng chứng người xem đã được xác thực. Trang Google kiểm tra phiên qua backend.

Khi triển khai chức năng sau đăng nhập:

1. Dùng API `/api/auth/me` đã có để tải người dùng hiện tại; `getCurrentUser` được xuất qua `auth/index.js`.
2. Khi nhiều trang cần chia sẻ trạng thái, tạo context/hook phiên đăng nhập trong `auth`, xuất giao diện cần thiết qua `auth/index.js`, ghép provider tại `app/providers`.
3. Thêm `ProtectedRoute.jsx` và `RoleRoute.jsx` trong `app/routes`; xử lý đủ trạng thái đang tải, chưa đăng nhập, sai vai trò và lỗi tải phiên.
4. Thêm `StudentLayout.jsx`, `TeacherLayout.jsx`, `AdminLayout.jsx` trong `app/layouts` khi có trang sử dụng.
5. Trang nghiệp vụ vẫn ở feature tương ứng, ví dụ `/teacher/classrooms` trỏ đến trang trong `features/classrooms/pages`.

### 2.8. API và dữ liệu

`shared/services/httpClient.js` nhận đường dẫn và tùy chọn `method`, `body`, `signal`, `headers`. Khi có body, nó gửi JSON; lỗi API giữ `status`, `code`, `fieldErrors` để feature hiển thị đúng thông báo. AbortError được giữ nguyên để component có thể bỏ qua yêu cầu đã hủy.

`features/auth/services/authApi.js` cung cấp các hàm có tên theo nghiệp vụ: `getRegistrationOptions`, `registerAccount`, `verifyRegistrationOtp`, `resendRegistrationOtp`, `getRegistrationSession`, `getCurrentUser`, `loginAccount`, `logoutAccount`. Login/logout tự lấy CSRF token rồi gửi header tương ứng; trình duyệt gửi cookie cùng origin qua Vite proxy.

Không lưu mật khẩu, OTP hoặc khóa bí mật trong browser storage. `registrationStorage` chỉ giữ email đang chờ xác minh trong sessionStorage. Biến `VITE_*` là dữ liệu có thể xuất hiện trong mã gửi đến trình duyệt, không dùng để chứa bí mật.

Backend hiện đã có nhiều entity/controller nhưng các API nghiệp vụ còn cần hoàn thiện và phân quyền; cấu hình SecurityConfig đang chặn các request ngoài các đường dẫn cho phép. Không mở rộng client sang các API này rồi coi chức năng đã tích hợp xong.

### 2.9. Cấu hình và sử dụng UC01–UC02


`JWT_SECRET` là Base64 của ít nhất 32 byte ngẫu nhiên. Tạo khóa bằng PowerShell rồi điền kết quả vào `backend/.env`:

Giữ khóa ở backend; không commit hoặc đưa vào biến `VITE_*`. Nếu không đặt khóa, backend dùng khóa tạm và phiên đăng nhập mất hiệu lực khi khởi động lại. Môi trường HTTPS dùng `SESSION_COOKIE_SECURE=true`; localhost HTTP dùng `false`.

Chạy từ thư mục gốc trong một terminal riêng:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Backend đọc `.env` theo thư mục chạy nên cần chạy từ `backend`. Cấu hình local `DB_DDL_AUTO=update` tạo bảng `login_sessions` dùng thu hồi phiên. Khi dùng schema validation ở môi trường triển khai, cần migration tạo bảng trước.

#### Đăng ký và đăng nhập bằng email


1. Mở `http://localhost:5173/register`, chọn Giáo viên/Học sinh và đăng ký.
2. Nhập OTP nhận qua email để kích hoạt tài khoản. OTP có hạn 5 phút; gửi lại cách nhau ít nhất 60 giây, tối đa 5 email/giờ; nhập sai tối đa 5 lần cho mỗi mã.
3. Vào `http://localhost:5173/login`, nhập email và mật khẩu.
4. Khi thành công, trang hiển thị tên, email, vai trò và nút **Đăng xuất**. Trang chủ hiển thị liên kết tài khoản.

Tài khoản chưa xác minh phải hoàn tất OTP trước khi đăng nhập; tài khoản bị khóa không được đăng nhập. Email/mật khẩu sai trả thông báo chung.

#### Đăng ký và đăng nhập Google


- **Đăng ký:** tại `/register`, chọn vai trò rồi nhấn **Tiếp tục với Google**. Tài khoản mới được tạo ở trạng thái `ACTIVE` và đăng nhập ngay.
- **Đăng nhập:** tại `/login`, nhấn **Đăng nhập với Google** bằng tài khoản Google đã đăng ký. Vai trò lấy từ database, không chọn lại khi đăng nhập.
- Chưa có tài khoản Google trong hệ thống: nhận thông báo đăng ký trước. Không tự liên kết tài khoản email chỉ vì trùng địa chỉ email; backend nhận diện Google bằng `googleSubject`.
- Google không cần SMTP/OTP của ứng dụng. Client Secret chỉ nằm ở backend.

#### Phiên đăng nhập và ghi nhớ

| Lựa chọn | Thời hạn JWT | Cookie |
| --- | --- | --- |
| Không ghi nhớ | 8 giờ | Cookie phiên trình duyệt |
| Ghi nhớ đăng nhập | 30 ngày | Cookie có Max-Age 30 ngày |

JWT được lưu trong cookie HttpOnly, SameSite=Lax; frontend không đọc JWT bằng JavaScript và không lưu JWT vào localStorage. Mỗi yêu cầu xác thực kiểm tra phiên, trạng thái và vai trò hiện tại trong database. Đăng xuất thu hồi phiên ngay, kể cả khi JWT cũ được gửi lại. Backend dọn phiên hết hạn khi phát JWT mới.

#### API xác thực được sử dụng

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| POST | `/api/auth/register` | Đăng ký email |
| POST | `/api/auth/verify-otp` | Xác minh OTP |
| POST | `/api/auth/resend-otp` | Gửi lại OTP |
| GET | `/api/auth/options` | Kiểm tra Google đã bật |
| GET | `/api/auth/registration-session` | Tải tài khoản sau đăng ký Google |
| GET | `/api/auth/csrf` | Lấy `token` và `headerName` cho login/logout |
| POST | `/api/auth/login` | Gửi `{ email, password, rememberMe }`; nhận tài khoản và cookie JWT |
| GET | `/api/auth/me` | Tải tài khoản của phiên hiện tại |
| POST | `/api/auth/logout` | Thu hồi phiên và xóa cookie |
| GET | `/api/auth/google?role=STUDENT` | Bắt đầu đăng ký Google; cũng nhận `TEACHER` |
| GET | `/api/auth/google-login?rememberMe=true` | Bắt đầu đăng nhập Google |

