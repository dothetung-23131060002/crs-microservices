// path: course-service/src/main/java/vn/edu/crs/courseservice/repository/CourseRepository.java
// purpose: repository JPA thao tac bang course, ke thua san CRUD tu Spring Data JPA
// Buoi 3: THAY THE TOAN BO file cu bang noi dung nay (chi them 1 dong Page<Course> ...)

package vn.edu.crs.courseservice.repository;

import vn.edu.crs.courseservice.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {

    boolean existsByTenMonHocIgnoreCase(String tenMonHoc);

    // Buoi 3: Spring Data JPA tu sinh cau lenh SQL LIKE %keyword% khong phan biet hoa/thuong
    Page<Course> findByTenMonHocContainingIgnoreCase(String keyword, Pageable pageable);
}
