// path: course-service/src/main/java/vn/edu/crs/courseservice/service/CourseService.java
// purpose: giu nguyen toan bo CRUD cua Buoi 2 (getAll/getById/create/update/delete),
//          bo sung: search co phan trang + reserveSeat/releaseSeat (Buoi 3)
//
// LUU Y: Neu CourseService.java hien tai cua ban khac cac ten phuong thuc getAll/getById/
// create/update/delete o duoi day, hay CHI COPY phan duoc danh dau "// ==== BUOI 3:" vao
// cuoi class cua ban, giu nguyen phan con lai.

package vn.edu.crs.courseservice.service;

import vn.edu.crs.courseservice.dto.CourseDTO;
import vn.edu.crs.courseservice.entity.Course;
import vn.edu.crs.courseservice.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;

    // ==== BUOI 2: CRUD co ban (giu nguyen) ====

    public List<CourseDTO> getAll() {
        return courseRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public CourseDTO getById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Khong tim thay mon hoc id = " + id));
        return toDTO(course);
    }

    public CourseDTO create(CourseDTO dto) {
        if (courseRepository.existsByTenMonHocIgnoreCase(dto.getTenMonHoc())) {
            throw new IllegalStateException("Mon hoc da ton tai: " + dto.getTenMonHoc());
        }
        Course course = new Course();
        course.setTenMonHoc(dto.getTenMonHoc());
        course.setSoChoToiDa(dto.getSoChoToiDa());
        course.setSoChoConLai(dto.getSoChoToiDa());
        return toDTO(courseRepository.save(course));
    }

    public CourseDTO update(Long id, CourseDTO dto) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Khong tim thay mon hoc id = " + id));
        course.setTenMonHoc(dto.getTenMonHoc());
        course.setSoChoToiDa(dto.getSoChoToiDa());
        return toDTO(courseRepository.save(course));
    }

    public void delete(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new NoSuchElementException("Khong tim thay mon hoc id = " + id);
        }
        courseRepository.deleteById(id);
    }

    private CourseDTO toDTO(Course course) {
        CourseDTO dto = new CourseDTO();
        dto.setId(course.getId());
        dto.setTenMonHoc(course.getTenMonHoc());
        dto.setSoChoToiDa(course.getSoChoToiDa());
        dto.setSoChoConLai(course.getSoChoConLai());
        return dto;
    }

    // ==== BUOI 3: search co phan trang ====

    public Page<CourseDTO> search(String keyword, Pageable pageable) {
        Page<Course> page = (keyword == null || keyword.isBlank())
                ? courseRepository.findAll(pageable)
                : courseRepository.findByTenMonHocContainingIgnoreCase(keyword, pageable);
        return page.map(this::toDTO);
    }

    // ==== BUOI 3: reserve-seat / release-seat (API noi bo) ====

    @Transactional
    public CourseDTO reserveSeat(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NoSuchElementException("Khong tim thay mon hoc id = " + courseId));
        if (course.getSoChoConLai() <= 0) {
            throw new IllegalStateException("Mon hoc da het cho, khong the dang ky");
        }
        course.setSoChoConLai(course.getSoChoConLai() - 1);
        return toDTO(courseRepository.save(course));
    }

    @Transactional
    public CourseDTO releaseSeat(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NoSuchElementException("Khong tim thay mon hoc id = " + courseId));
        if (course.getSoChoConLai() < course.getSoChoToiDa()) {
            course.setSoChoConLai(course.getSoChoConLai() + 1);
        }
        return toDTO(courseRepository.save(course));
    }
}
