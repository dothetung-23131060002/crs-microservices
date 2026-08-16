package vn.edu.crs.authservice.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.crs.authservice.entity.Student;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByMssv(String mssv);
}
