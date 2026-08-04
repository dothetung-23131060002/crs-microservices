package vn.edu.crs.courseservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên môn học không được để trống")
    @Column(nullable = false)
    private String name;

    private String description;

    @NotNull(message = "Số tín chỉ không được để trống")
    private Integer credits;

    @Column(name = "so_cho_con_lai")
    private Integer soChoConLai;

    @Column(name = "total_slots")
    private Integer totalSlots;
}
