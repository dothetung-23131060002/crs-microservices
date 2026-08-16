package vn.edu.crs.registrationservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequestDTO {

    @NotNull(message = "studentId khong duoc de trong")
    @Positive(message = "studentId phai lon hon 0")
    private Long studentId;

    @NotNull(message = "courseId khong duoc de trong")
    @Positive(message = "courseId phai lon hon 0")
    private Long courseId;
}
