package vn.edu.crs.registrationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "registration",
        indexes = {
                @Index(name = "idx_registration_student", columnList = "student_id"),
                @Index(name = "idx_registration_student_course_status", columnList = "student_id,course_id,trang_thai")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    // Course belongs to course_db, so this is a logical reference rather than a JPA relationship.
    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "trang_thai", nullable = false, length = 20)
    private String trangThai;

    @Column(name = "ngay_dang_ky", nullable = false)
    private LocalDateTime ngayDangKy;
}
