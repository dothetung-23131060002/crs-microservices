// path: course-service/src/main/java/vn/edu/crs/courseservice/repository/CourseRepository.java
// purpose: repository JPA thao tac bang course, ke thua san CRUD tu Spring Data JPA
package vn.edu.crs.courseservice.repository;

import vn.edu.crs.courseservice.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
    boolean existsByTenMonHocIgnoreCase(String tenMonHoc);
}