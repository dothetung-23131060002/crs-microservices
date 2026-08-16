# Blueprint API — Hệ thống CRS Microservices

> Tài liệu này liệt kê contract đã triển khai đến hết Buổi 4,
> bao gồm cả API nội bộ giữa các service. Endpoint dự kiến từ blueprint Buổi 1
> nhưng chưa thuộc phạm vi triển khai của bốn buổi được ghi rõ là **Planned**.

---

## 1. auth-service (cổng 8081)

**Tiền tố khi qua Gateway:** `/api/auth`

| Method | Endpoint          | Mô tả                        | Yêu cầu xác thực |
|--------|-------------------|-------------------------------|-------------------|
| POST   | `/auth/login`     | Đăng nhập, trả về JWT token  | Public            |

### Chi tiết:
- **POST /auth/login**
  - Request Body: `{ "username": "string", "password": "string" }`
  - Response: `{ "token": "jwt-string", "username": "admin", "role": "ADMIN" }`
  - HTTP 200: Đăng nhập thành công
  - HTTP 401: Sai username/password

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
  - Query params: `?keyword=laptrinh&page=0&size=10&sort=tenMonHoc,asc`
  - Alias `search` vẫn được chấp nhận để tương thích blueprint ban đầu.
  - Response: Spring Data `Page<CourseDTO>` với mảng dữ liệu trong trường `content`
  - HTTP 200: Trả về danh sách

- **GET /courses/{id}**
  - Path variable: `id` (Long)
  - Response: Đối tượng Course
  - HTTP 200: Tìm thấy
  - HTTP 404: Không tìm thấy

- **POST /courses**
  - Request Body: `{ "tenMonHoc": "Lap trinh Java", "soTinChi": 3, "soChoToiDa": 50 }`
  - Response: `CourseDTO`; server tự đặt `soChoConLai = soChoToiDa`
  - HTTP 201: Tạo thành công

- **PUT /courses/{id}**
  - Request Body: Tương tự POST
  - `soChoConLai` chỉ thay đổi qua reserve/release, không nhận từ request cập nhật.
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
  - Tăng `soChoConLai += 1` (không vượt quá `soChoToiDa`)
  - HTTP 200: Hoàn trả chỗ thành công
  - HTTP 404: Không tìm thấy course

---

## 3. registration-service (cổng 8083)

**Tiền tố khi qua Gateway:** `/api/registrations`

| Method | Endpoint               | Mô tả                                                          | Yêu cầu xác thực |
|--------|------------------------|-----------------------------------------------------------------|-------------------|
| POST   | `/registrations`       | Đăng ký học phần (gọi ngầm sang course-service reserve-seat)   | STUDENT           |
| GET    | `/registrations/my`    | Danh sách đăng ký của sinh viên hiện tại — **Planned**          | STUDENT           |
| DELETE | `/registrations/{id}`  | Huỷ đăng ký (gọi ngầm release-seat sang course-service)       | STUDENT / ADMIN   |

### Chi tiết:
- **POST /registrations**
  - Request Body: `{ "studentId": 1, "courseId": 1 }`
  - Quy trình nội bộ:
    1. Gọi `PATCH /internal/courses/{courseId}/reserve-seat` sang course-service
    2. Nếu thành công → lưu Registration vào DB
    3. Nếu hết chỗ → trả lỗi 409
  - Response: Đối tượng Registration
  - HTTP 201: Đăng ký thành công
  - HTTP 409: Hết chỗ

- **GET /registrations/my — Planned**
  - Thuộc blueprint tối thiểu của Buổi 1; chưa được yêu cầu triển khai trong phạm vi Buổi 1–4.
  - Khi triển khai, `studentId` phải được lấy từ danh tính đã xác thực thay vì tin dữ liệu client gửi lên.

- **DELETE /registrations/{id}**
  - Quy trình nội bộ:
    1. Gọi `PATCH /internal/courses/{courseId}/release-seat` sang course-service
    2. Chỉ sau khi hoàn chỗ thành công mới đổi trạng thái sang `DA_HUY`
  - HTTP 200: Huỷ thành công
  - HTTP 409: Bản ghi đã được huỷ trước đó
  - HTTP 404: Không tìm thấy đăng ký

---

## 4. Tổng hợp tất cả Endpoint

| # | Service              | Method | Endpoint                                | Loại     |
|---|----------------------|--------|-----------------------------------------|----------|
| 1 | auth-service         | POST   | `/auth/login`                           | Public   |
| 2 | course-service       | GET    | `/courses`                              | Public   |
| 3 | course-service       | GET    | `/courses/{id}`                         | Public   |
| 4 | course-service       | POST   | `/courses`                              | ADMIN    |
| 5 | course-service       | PUT    | `/courses/{id}`                         | ADMIN    |
| 6 | course-service       | DELETE | `/courses/{id}`                         | ADMIN    |
| 7 | course-service       | PATCH  | `/internal/courses/{id}/reserve-seat`   | Internal |
| 8 | course-service       | PATCH  | `/internal/courses/{id}/release-seat`   | Internal |
| 9 | registration-service | POST   | `/registrations`                        | STUDENT  |
| 10| registration-service | GET    | `/registrations/my`                     | STUDENT (Planned) |
| 11| registration-service | DELETE | `/registrations/{id}`                   | STUDENT/ADMIN |
