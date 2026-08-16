# Báo cáo kiểm chứng Buổi 1–4

Kết quả gần nhất được tạo lúc `2026-08-16T13:10:03+07:00` bằng:

```powershell
.\scripts\verify-all.ps1
```

Kịch bản đã chạy đầy đủ từ bước `clean package`, khởi động bốn JAR với MySQL thật, chạy bốn collection bằng Newman, kiểm tra chuyển trạng thái trong database và thử tình huống `course-service` ngừng hoạt động. Kết quả tổng hợp là **PASS**.

Môi trường JDK, IntelliJ/Lombok/annotation processing, MySQL/Workbench, Postman Desktop và Git được ghi tại [`environment-evidence.txt`](reports/environment-evidence.txt).

## Test Maven

| Service | Test | Failure | Error | Skipped |
|---|---:|---:|---:|---:|
| auth-service | 6 | 0 | 0 | 0 |
| course-service | 10 | 0 | 0 | 0 |
| registration-service | 19 | 0 | 0 | 0 |
| api-gateway | 6 | 0 | 0 | 0 |
| **Tổng** | **41** | **0** | **0** | **0** |

Chi tiết máy đọc được: [`maven-tests.json`](reports/maven-tests.json).

## Collection Postman/Newman

| Collection | Request chạy | Assertion | Thất bại |
|---|---:|---:|---:|
| auth-service | 4 | 8 | 0 |
| course-service | 18 | 23 | 0 |
| registration-service | 15 | 16 | 0 |
| api-gateway | 19 | 30 | 0 |

CLI report: [`auth-service.txt`](reports/auth-service.txt), [`course-service.txt`](reports/course-service.txt), [`registration-service.txt`](reports/registration-service.txt), [`api-gateway.txt`](reports/api-gateway.txt). Mỗi collection cũng có JUnit XML cùng tên trong thư mục `reports`.

Request `MANUAL ONLY` trong registration collection được bỏ qua ở lượt chạy bình thường. Script tự dừng `course-service` và kiểm tra riêng: HTTP `503`, đúng thông báo, phản hồi trong `90 ms`. Bằng chứng: [`course-service-unavailable.json`](reports/course-service-unavailable.json).

## MySQL

[`database-evidence.txt`](reports/database-evidence.txt) xác nhận ba schema `auth_db`, `course_db`, `registration_db`, hai tài khoản mẫu và mật khẩu được lưu dạng BCrypt.

[`database-transitions.txt`](reports/database-transitions.txt) lưu trực tiếp năm checkpoint SQL trên MySQL:

1. Course được tạo với `remaining=1`.
2. Course được cập nhật nhưng số chỗ vẫn là `1`.
3. Sau đăng ký: `remaining=0`, trạng thái `DA_DANG_KY`.
4. Sau hủy: `remaining=1`, trạng thái `DA_HUY`.
5. Sau cleanup: course thử nghiệm có `row_count=0`.

## Tổng kết

[`verification-summary.json`](reports/verification-summary.json) xác nhận JDK/Javac `17.0.12`, `41` test Maven, `4` collection Newman, `5` checkpoint database và trạng thái `PASS`. Sau khi hoàn tất, script đã dừng đúng các process do nó tạo; các cổng `8080–8083` không còn listener thử nghiệm.
