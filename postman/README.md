# Postman workspace

Workspace đích theo Buổi 1 có tên **CRS Microservices**. Trong Workspace đó, import bốn collection v2.1 sau; trường `info.name` đã được đặt đúng tên hiển thị:

| Collection | File | Cổng dùng |
|---|---|---:|
| `auth-service` | [`auth-service.postman_collection.json`](auth-service.postman_collection.json) | 8081 |
| `course-service` | [`course-service.postman_collection.json`](course-service.postman_collection.json) | 8081, 8082 |
| `registration-service` | [`registration-service.postman_collection.json`](registration-service.postman_collection.json) | 8081, 8082, 8083 |
| `api-gateway` | [`CRS-Microservices.postman_collection.json`](CRS-Microservices.postman_collection.json) | chỉ 8080 |

Có thể chọn đồng thời cả bốn file trong hộp thoại **Import**, hoặc kéo toàn bộ thư mục `postman` vào Postman Desktop. Chạy các request theo thứ tự đã đánh số trong từng collection.

Collection `api-gateway` là bộ test Buổi 4; tất cả URL request của collection này chỉ dùng `http://localhost:8080`. Ba collection direct chỉ được giữ cho các checkpoint Buổi 1–3 và không được dùng làm test Buổi 4.

Kết quả chạy tự động gần nhất nằm tại [`../verification/reports`](../verification/reports). Từ thư mục gốc repository, có thể tái tạo toàn bộ bằng lệnh `.\scripts\verify-all.ps1`.
