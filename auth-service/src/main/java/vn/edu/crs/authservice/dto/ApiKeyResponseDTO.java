// path: auth-service/src/main/java/vn/edu/crs/authservice/dto/ApiKeyResponseDTO.java
// purpose: DTO tra ve thong tin 1 API Key (dung cho danh sach va sau khi cap moi)
package vn.edu.crs.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyResponseDTO {
    private Long id;
    private String keyValue;
    private String ownerName;
    private String scopes;
    private String status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
