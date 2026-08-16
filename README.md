# CRS Microservices

Hệ thống minh hoạ đăng ký học phần gồm bốn ứng dụng Spring Boot độc lập:

| Ứng dụng | Cổng | Database | Trách nhiệm |
|---|---:|---|---|
| `api-gateway` | 8080 | — | Điểm vào duy nhất, routing, CORS, chặn sớm Authorization/API key |
| `auth-service` | 8081 | `auth_db` | Xác thực tài khoản và cấp JWT |
| `course-service` | 8082 | `course_db` | CRUD, tìm kiếm/phân trang và quản lý số chỗ |
| `registration-service` | 8083 | `registration_db` | Đăng ký/hủy học phần và gọi `course-service` |

## Yêu cầu

- JDK 17
- MySQL 8 chạy trên `localhost:3306`
- Maven Wrapper trong `course-service` (có thể dùng wrapper này với `-f` cho các service khác)

Tạo database nếu chưa có:

```sql
CREATE DATABASE IF NOT EXISTS course_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS registration_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Biến môi trường có thể ghi đè cấu hình local:

```text
DB_HOST, DB_PORT, DB_USERNAME, DB_PASSWORD, DB_NAME
COURSE_DB_NAME, COURSE_DB_USERNAME, COURSE_DB_PASSWORD
JWT_SECRET
AUTH_SERVICE_URL, COURSE_SERVICE_URL, REGISTRATION_SERVICE_URL
PARTNER_API_KEY
```

> `JWT_SECRET` và mật khẩu database mặc định chỉ phục vụ bài lab. Phải thay bằng secret manager/biến môi trường trước khi triển khai thật.

## Chạy hệ thống

Mở bốn terminal tại thư mục gốc repository:

```powershell
.\course-service\mvnw.cmd -f .\auth-service\pom.xml spring-boot:run
.\course-service\mvnw.cmd -f .\course-service\pom.xml spring-boot:run
.\course-service\mvnw.cmd -f .\registration-service\pom.xml spring-boot:run
.\course-service\mvnw.cmd -f .\api-gateway\pom.xml spring-boot:run
```

Từ Buổi 4, client chỉ gọi qua `http://localhost:8080`. Các API `/internal/**` không có route tại Gateway.

Tài khoản mẫu được tạo idempotent khi `auth-service` khởi động:

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | `ADMIN` |
| `student1` | `student123` | `STUDENT` |

API key đối tác local: `crs-partner-key-2026`.

## Kiểm thử

```powershell
.\course-service\mvnw.cmd -f .\course-service\pom.xml test
.\course-service\mvnw.cmd -f .\registration-service\pom.xml test
.\course-service\mvnw.cmd -f .\auth-service\pom.xml test
.\course-service\mvnw.cmd -f .\api-gateway\pom.xml test
```

Import collection [`postman/CRS-Microservices.postman_collection.json`](postman/CRS-Microservices.postman_collection.json) và chạy theo thứ tự thư mục trong collection để kiểm tra luồng qua Gateway.

## Giới hạn giao dịch phân tán

Luồng đăng ký trừ chỗ tại `course-service` trước, sau đó mới lưu `Registration`. Nếu database của `registration-service` lỗi sau khi đã trừ chỗ, dữ liệu có thể lệch. Bài lab chấp nhận giới hạn này; hệ thống thực tế nên dùng Saga/Outbox và retry idempotent.
