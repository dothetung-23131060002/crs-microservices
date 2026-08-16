package vn.edu.crs.courseservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.crs.courseservice.dto.CourseDTO;
import vn.edu.crs.courseservice.entity.Course;
import vn.edu.crs.courseservice.repository.CourseRepository;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;

    public List<CourseDTO> getAll() {
        return courseRepository.findAll().stream().map(this::toDTO).toList();
    }

    public Page<CourseDTO> search(String keyword, Pageable pageable) {
        Page<Course> courses = keyword == null || keyword.isBlank()
                ? courseRepository.findAll(pageable)
                : courseRepository.findByTenMonHocContainingIgnoreCase(keyword.trim(), pageable);
        return courses.map(this::toDTO);
    }

    public CourseDTO getById(Long id) {
        return toDTO(findById(id));
    }

    @Transactional
    public CourseDTO create(CourseDTO dto) {
        String tenMonHoc = normalizeName(dto.getTenMonHoc());
        if (courseRepository.existsByTenMonHocIgnoreCase(tenMonHoc)) {
            throw new IllegalArgumentException("Ten mon hoc da ton tai");
        }

        Course course = Course.builder()
                .tenMonHoc(tenMonHoc)
                .soTinChi(dto.getSoTinChi())
                .soChoToiDa(dto.getSoChoToiDa())
                .soChoConLai(dto.getSoChoToiDa())
                .build();
        return toDTO(courseRepository.save(course));
    }

    @Transactional
    public CourseDTO update(Long id, CourseDTO dto) {
        Course course = findById(id);
        String tenMonHoc = normalizeName(dto.getTenMonHoc());
        if (courseRepository.existsByTenMonHocIgnoreCaseAndIdNot(tenMonHoc, id)) {
            throw new IllegalArgumentException("Ten mon hoc da ton tai");
        }
        if (dto.getSoChoToiDa() < course.getSoChoConLai()) {
            throw new IllegalArgumentException("So cho toi da khong duoc nho hon so cho con lai");
        }

        course.setTenMonHoc(tenMonHoc);
        course.setSoTinChi(dto.getSoTinChi());
        course.setSoChoToiDa(dto.getSoChoToiDa());
        // soChoConLai is only changed by reserve-seat/release-seat.
        return toDTO(courseRepository.save(course));
    }

    @Transactional
    public void delete(Long id) {
        if (!courseRepository.existsById(id)) {
            throw notFound(id);
        }
        courseRepository.deleteById(id);
    }

    @Transactional
    public CourseDTO reserveSeat(Long courseId) {
        Course course = findByIdForUpdate(courseId);
        if (course.getSoChoConLai() == null || course.getSoChoConLai() <= 0) {
            throw new IllegalStateException("Mon hoc da het cho, khong the dang ky");
        }
        course.setSoChoConLai(course.getSoChoConLai() - 1);
        return toDTO(courseRepository.save(course));
    }

    @Transactional
    public CourseDTO releaseSeat(Long courseId) {
        Course course = findByIdForUpdate(courseId);
        if (course.getSoChoConLai() == null) {
            throw new IllegalStateException("Du lieu so cho con lai khong hop le");
        }
        if (course.getSoChoConLai() < course.getSoChoToiDa()) {
            course.setSoChoConLai(course.getSoChoConLai() + 1);
            course = courseRepository.save(course);
        }
        return toDTO(course);
    }

    private Course findById(Long id) {
        return courseRepository.findById(id).orElseThrow(() -> notFound(id));
    }

    private Course findByIdForUpdate(Long id) {
        return courseRepository.findByIdForUpdate(id).orElseThrow(() -> notFound(id));
    }

    private NoSuchElementException notFound(Long id) {
        return new NoSuchElementException("Khong tim thay mon hoc id = " + id);
    }

    private String normalizeName(String tenMonHoc) {
        return tenMonHoc == null ? null : tenMonHoc.trim();
    }

    private CourseDTO toDTO(Course course) {
        return CourseDTO.builder()
                .id(course.getId())
                .tenMonHoc(course.getTenMonHoc())
                .soTinChi(course.getSoTinChi())
                .soChoToiDa(course.getSoChoToiDa())
                .soChoConLai(course.getSoChoConLai())
                .build();
    }
}
