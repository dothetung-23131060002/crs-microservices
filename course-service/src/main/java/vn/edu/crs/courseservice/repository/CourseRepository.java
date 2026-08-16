package vn.edu.crs.courseservice.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.crs.courseservice.entity.Course;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    boolean existsByTenMonHocIgnoreCase(String tenMonHoc);

    boolean existsByTenMonHocIgnoreCaseAndIdNot(String tenMonHoc, Long id);

    Page<Course> findByTenMonHocContainingIgnoreCase(String keyword, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select course from Course course where course.id = :id")
    Optional<Course> findByIdForUpdate(@Param("id") Long id);
}
