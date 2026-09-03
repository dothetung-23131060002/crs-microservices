// path: auth-service/src/main/java/vn/edu/crs/authservice/dto/ApiKeyCreateRequestDTO.java
// purpose: DTO nhan du lieu khi ADMIN cap moi 1 API Key
package vn.edu.crs.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApiKeyCreateRequestDTO {
    @NotBlank(message = "Ten doi tac khong duoc de trong")
    private String ownerName;

    @NotBlank(message = "Danh sach scope khong duoc de trong")
    private String scopes; // vi du: "courses:read"

    private Integer validDays; // so ngay hieu luc; null = khong gioi han
}
