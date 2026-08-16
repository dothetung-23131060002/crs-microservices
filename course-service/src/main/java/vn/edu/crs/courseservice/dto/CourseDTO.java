package vn.edu.crs.courseservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {

    private Long id;

    @NotBlank(message = "Ten mon hoc khong duoc de trong")
    @Size(max = 255, message = "Ten mon hoc khong duoc vuot qua 255 ky tu")
    private String tenMonHoc;

    @NotNull(message = "So tin chi khong duoc de trong")
    @Min(value = 1, message = "So tin chi phai lon hon 0")
    private Integer soTinChi;

    @NotNull(message = "So cho toi da khong duoc de trong")
    @Min(value = 1, message = "So cho toi da phai lon hon 0")
    private Integer soChoToiDa;

    private Integer soChoConLai;
}
