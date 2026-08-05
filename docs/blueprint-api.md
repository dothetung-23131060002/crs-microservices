# Blueprint API — Hệ thống CRS Microservices

> Tài liệu này liệt kê toàn bộ endpoint dự kiến cho cả hệ thống,
> bao gồm cả API nội bộ giữa các service.
> Blueprint sẽ được cập nhật dần qua các buổi học.

---

## 1. auth-service (cổng 8081)

**Tiền tố khi qua Gateway:** `/api/auth`

| Method | Endpoint          | Mô tả                        | Yêu cầu xác thực |
|--------|-------------------|-------------------------------|-------------------|
| POST   | `/auth/login`     | Đăng nhập, trả về JWT token  | Public            |
| POST   | `/auth/register`  | Đăng ký tài khoản mới (tuỳ chọn) | Public        |

### Chi tiết:
- **POST /auth/login**
  - Request Body: `{ "username": "string", "password": "string" }`
  - Response: `{ "token": "jwt-string", "type": "Bearer", "expiresIn": 3600 }`
  - HTTP 200: Đăng nhập thành công
  - HTTP 401: Sai username/password

- **POST /auth/register**
  - Request Body: `{ "username": "string", "password": "string", "fullName": "string", "email": "string" }`
  - Response: `{ "id": 1, "username": "string", "message": "Đăng ký thành công" }`
  - HTTP 201: Tạo tài khoản thành công
  - HTTP 400: Dữ liệu không hợp lệ hoặc username đã tồn tại

---

## 2. course-service (cổng 8082)

**Tiền tố khi qua Gateway:** `/api/courses`

### 2.1. API công khai (Public / ADMIN)

| Method | Endpoint          | Mô tả                                    | Yêu cầu xác thực |
|--------|-------------------|-------------------------------------------|-------------------|
| GET    | `/courses`        | Danh sách môn học, hỗ trợ search + phân trang | Public         |
| GET    | `/courses/{id}`   | Chi tiết 1 môn học                        | Public            |
| POST   | `/courses`        | Thêm môn học mới                          | ADMIN             |
| PUT    | `/courses/{id}`   | Sửa thông tin môn học                     | ADMIN             |
| DELETE | `/courses/{id}`   | Xoá môn học                               | ADMIN             |

### Chi tiết:
- **GET /courses**
  - Query params: `?search=keyword&page=0&size=10`
  - Response: Mảng JSON các đối tượng Course
  - HTTP 200: Trả về danh sách

- **GET /courses/{id}**
  - Path variable: `id` (Long)
  - Response: Đối tượng Course
  - HTTP 200: Tìm thấy
  - HTTP 404: Không tìm thấy

- **POST /courses**
  - Request Body: `{ "name": "string", "description": "string", "credits": 3, "totalSlots": 50 }`
  - Response: Đối tượng Course vừa tạo
  - HTTP 201: Tạo thành công

- **PUT /courses/{id}**
  - Request Body: Tương tự POST
  - HTTP 200: Cập nhật thành công
  - HTTP 404: Không tìm thấy

- **DELETE /courses/{id}**
  - HTTP 204: Xoá thành công
  - HTTP 404: Không tìm thấy

### 2.2. API nội bộ (Internal — chỉ gọi từ registration-service)

> ⚠️ Các endpoint này **KHÔNG** được lộ ra Gateway cho Frontend.
> Chỉ cho phép gọi trực tiếp giữa các service trong mạng nội bộ.

| Method | Endpoint                                | Mô tả                                                        |
|--------|-----------------------------------------|---------------------------------------------------------------|
| PATCH  | `/internal/courses/{id}/reserve-seat`   | Kiểm tra còn chỗ, trừ `soChoConLai` đi 1 (transactional)    |
| PATCH  | `/internal/courses/{id}/release-seat`   | Hoàn trả 1 chỗ khi huỷ đăng ký, tăng `soChoConLai` lên 1    |

### Chi tiết:
- **PATCH /internal/courses/{id}/reserve-seat**
  - Kiểm tra `soChoConLai > 0`, nếu còn chỗ thì `soChoConLai -= 1`
  - HTTP 200: Đặt chỗ thành công
  - HTTP 409 (Conflict): Hết chỗ

- **PATCH /internal/courses/{id}/release-seat**
  - Tăng `soChoConLai += 1` (không vượt quá `totalSlots`)
  - HTTP 200: Hoàn trả chỗ thành công
  - HTTP 404: Không tìm thấy course

---

## 3. registration-service (cổng 8083)

**Tiền tố khi qua Gateway:** `/api/registrations`

| Method | Endpoint               | Mô tả                                                          | Yêu cầu xác thực |
|--------|------------------------|-----------------------------------------------------------------|-------------------|
| POST   | `/registrations`       | Đăng ký học phần (gọi ngầm sang course-service reserve-seat)   | STUDENT           |
| GET    | `/registrations/my`    | Danh sách đăng ký của sinh viên đang đăng nhập                 | STUDENT           |
| DELETE | `/registrations/{id}`  | Huỷ đăng ký (gọi ngầm release-seat sang course-service)       | STUDENT / ADMIN   |

### Chi tiết:
- **POST /registrations**
  - Request Body: `{ "courseId": 1 }`
  - Quy trình nội bộ:
    1. Gọi `PATCH /internal/courses/{courseId}/reserve-seat` sang course-service
    2. Nếu thành công → lưu Registration vào DB
    3. Nếu hết chỗ → trả lỗi 409
  - Response: Đối tượng Registration
  - HTTP 201: Đăng ký thành công
  - HTTP 409: Hết chỗ

- **GET /registrations/my**
  - Lấy từ JWT token → xác định studentId
  - Response: Mảng JSON các Registration của sinh viên
  - HTTP 200: Trả về danh sách

- **DELETE /registrations/{id}**
  - Quy trình nội bộ:
    1. Xoá Registration khỏi DB
    2. Gọi `PATCH /internal/courses/{courseId}/release-seat` sang course-service
  - HTTP 204: Huỷ thành công
  - HTTP 404: Không tìm thấy đăng ký

---

## 4. Tổng hợp tất cả Endpoint

| # | Service              | Method | Endpoint                                | Loại     |
|---|----------------------|--------|-----------------------------------------|----------|
| 1 | auth-service         | POST   | `/auth/login`                           | Public   |
| 2 | auth-service         | POST   | `/auth/register`                        | Public   |
| 3 | course-service       | GET    | `/courses`                              | Public   |
| 4 | course-service       | GET    | `/courses/{id}`                         | Public   |
| 5 | course-service       | POST   | `/courses`                              | ADMIN    |
| 6 | course-service       | PUT    | `/courses/{id}`                         | ADMIN    |
| 7 | course-service       | DELETE | `/courses/{id}`                         | ADMIN    |
| 8 | course-service       | PATCH  | `/internal/courses/{id}/reserve-seat`   | Internal |
| 9 | course-service       | PATCH  | `/internal/courses/{id}/release-seat`   | Internal |
| 10| registration-service | POST   | `/registrations`                        | STUDENT  |
| 11| registration-service | GET    | `/registrations/my`                     | STUDENT  |
| 12| registration-service | DELETE | `/registrations/{id}`                   | STUDENT/ADMIN |
