package vn.edu.crs.courseservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import vn.edu.crs.courseservice.dto.CourseDTO;
import vn.edu.crs.courseservice.entity.Course;
import vn.edu.crs.courseservice.repository.CourseRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseService courseService;

    @Test
    void createInitializesRemainingSeatsFromMaximumAndIgnoresClientValue() {
        CourseDTO request = CourseDTO.builder()
                .tenMonHoc("  Lap trinh Java  ")
                .soTinChi(3)
                .soChoToiDa(40)
                .soChoConLai(2)
                .build();
        when(courseRepository.existsByTenMonHocIgnoreCase("Lap trinh Java")).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        CourseDTO created = courseService.create(request);

        assertThat(created.getId()).isEqualTo(1L);
        assertThat(created.getTenMonHoc()).isEqualTo("Lap trinh Java");
        assertThat(created.getSoChoConLai()).isEqualTo(40);
    }

    @Test
    void updatePreservesRemainingSeats() {
        Course stored = course(1L, "Lap trinh Java", 3, 40, 17);
        CourseDTO request = CourseDTO.builder()
                .tenMonHoc("Lap trinh huong dich vu")
                .soTinChi(4)
                .soChoToiDa(45)
                .soChoConLai(1)
                .build();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(stored));
        when(courseRepository.existsByTenMonHocIgnoreCaseAndIdNot(request.getTenMonHoc(), 1L))
                .thenReturn(false);
        when(courseRepository.save(stored)).thenReturn(stored);

        CourseDTO updated = courseService.update(1L, request);

        assertThat(updated.getSoChoConLai()).isEqualTo(17);
        assertThat(updated.getSoChoToiDa()).isEqualTo(45);
        assertThat(updated.getTenMonHoc()).isEqualTo("Lap trinh huong dich vu");
    }

    @Test
    void searchUsesCaseInsensitiveRepositoryQueryWhenKeywordIsPresent() {
        PageRequest pageable = PageRequest.of(0, 5);
        Course stored = course(1L, "Lap trinh Java", 3, 40, 40);
        when(courseRepository.findByTenMonHocContainingIgnoreCase("java", pageable))
                .thenReturn(new PageImpl<>(List.of(stored), pageable, 1));

        var result = courseService.search("  java  ", pageable);

        assertThat(result.getTotalElements()).isOne();
        assertThat(result.getContent().get(0).getTenMonHoc()).isEqualTo("Lap trinh Java");
        verify(courseRepository).findByTenMonHocContainingIgnoreCase("java", pageable);
    }

    @Test
    void reserveSeatRejectsCourseWithoutRemainingSeats() {
        Course stored = course(1L, "Kien truc phan mem", 3, 30, 0);
        when(courseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> courseService.reserveSeat(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Mon hoc da het cho, khong the dang ky");
        verify(courseRepository, never()).save(any());
    }

    @Test
    void reserveAndReleaseKeepRemainingSeatsWithinBounds() {
        Course stored = course(1L, "Kien truc phan mem", 3, 2, 1);
        when(courseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(stored));
        when(courseRepository.save(stored)).thenReturn(stored);

        CourseDTO reserved = courseService.reserveSeat(1L);
        CourseDTO released = courseService.releaseSeat(1L);

        assertThat(reserved.getSoChoConLai()).isZero();
        assertThat(released.getSoChoConLai()).isEqualTo(1);
    }

    private Course course(
            Long id,
            String tenMonHoc,
            int soTinChi,
            int soChoToiDa,
            int soChoConLai) {
        return Course.builder()
                .id(id)
                .tenMonHoc(tenMonHoc)
                .soTinChi(soTinChi)
                .soChoToiDa(soChoToiDa)
                .soChoConLai(soChoConLai)
                .build();
    }
}
