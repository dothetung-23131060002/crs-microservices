// path: registration-service/src/main/java/vn/edu/crs/registrationservice/dto/RegistrationRequestDTO.java
// purpose: DTO nhan du lieu dau vao khi sinh vien dang ky hoc phan

package vn.edu.crs.registrationservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegistrationRequestDTO {

    @NotNull(message = "studentId khong duoc de trong")
    private Long studentId;

    @NotNull(message = "courseId khong duoc de trong")
    private Long courseId;
}
