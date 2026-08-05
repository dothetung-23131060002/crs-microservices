# Thiết kế biên giới Service — Hệ thống Đăng ký Học phần (CRS Microservices)

> Tài liệu này mô tả ranh giới (bounded context) của từng service trong hệ thống CRS,
> nguyên tắc sở hữu dữ liệu, và bảng định tuyến gateway dự kiến.

---

## 1. Danh sách Service

| Service                  | Cổng | Database            | Trách nhiệm chính                                                                                 |
|--------------------------|------|---------------------|----------------------------------------------------------------------------------------------------|
| **api-gateway**          | 8080 | *(không có DB)*     | Điểm vào duy nhất, định tuyến request tới các service phía sau, xác thực sơ bộ JWT, xử lý CORS   |
| **auth-service**         | 8081 | `auth_db`           | Quản lý User, Student, đăng nhập, đăng ký tài khoản, sinh và xác thực JWT                        |
| **course-service**       | 8082 | `course_db`         | Quản lý Course (CRUD), tìm kiếm, phân trang, quản lý số chỗ còn lại (`soChoConLai`)              |
| **registration-service** | 8083 | `registration_db`   | Quản lý Registration (đăng ký học phần), gọi sang course-service để đặt/hoàn trả chỗ              |

### Sơ đồ tổng quan

```
                    ┌──────────────────┐
   Client/Frontend  │   api-gateway    │  :8080
        ──────────► │  (routing, JWT)  │
                    └──────┬───┬───┬───┘
                           │   │   │
              ┌────────────┘   │   └────────────┐
              ▼                ▼                 ▼
     ┌────────────┐   ┌──────────────┐   ┌──────────────────┐
     │auth-service│   │course-service│   │registration-     │
     │   :8081    │   │    :8082     │   │  service :8083   │
     │  auth_db   │   │  course_db   │   │ registration_db  │
     └────────────┘   └──────────────┘   └──────┬───────────┘
                                                │
                                      REST API  │  (internal)
                                                ▼
                                        ┌──────────────┐
                                        │course-service│
                                        │reserve/release│
                                        └──────────────┘
```

---

## 2. Nguyên tắc sở hữu dữ liệu (Data Ownership)

1. **Mỗi service có DATABASE RIÊNG** — Không service nào được truy cập trực tiếp DB của service khác.

2. **Giao tiếp qua REST API** — Muốn lấy hoặc thay đổi dữ liệu của service khác → PHẢI gọi REST API sang service đó.

3. **Chỉ lưu khóa tham chiếu (Foreign Key logic)** — Ví dụ cụ thể:
   - `registration-service` **KHÔNG** có bảng `Course`, chỉ lưu `courseId` (kiểu số, không có khóa ngoại thật trên database).
   - Khi cần thông tin chi tiết môn học, `registration-service` sẽ gọi `GET /courses/{id}` sang `course-service`.

4. **Transactional boundary** — Mỗi service tự quản lý transaction trong DB của mình. Không có distributed transaction xuyên suốt nhiều service (ở mức học tập này).

---

## 3. Bảng định tuyến Gateway (dự kiến)

| Route                   | Forward tới              | Ghi chú                                           |
|-------------------------|--------------------------|----------------------------------------------------|
| `/api/auth/**`          | `http://localhost:8081`   | `POST /auth/login` là Public, các endpoint còn lại cần JWT |
| `/api/courses/**`       | `http://localhost:8082`   | `GET` là Public, `POST/PUT/DELETE` cần role `ADMIN` |
| `/api/registrations/**` | `http://localhost:8083`   | Cần JWT (role `STUDENT` hoặc `ADMIN`)              |
| `/api/public/courses`   | `http://localhost:8082`   | Dùng API Key, dành cho đối tác bên ngoài           |

### Lưu ý:
- Các endpoint nội bộ (`/internal/**`) **KHÔNG** được định tuyến qua Gateway — chỉ cho phép gọi trực tiếp giữa các service trong mạng nội bộ.
- Gateway sẽ được triển khai bằng Spring Cloud Gateway ở buổi sau.

---

## 4. Tóm tắt phân chia dữ liệu

| Database            | Service sở hữu         | Bảng chính dự kiến                |
|---------------------|-------------------------|-----------------------------------|
| `auth_db`           | auth-service            | `users`, `roles`, `user_roles`    |
| `course_db`         | course-service          | `courses`                         |
| `registration_db`   | registration-service    | `registrations`                   |
