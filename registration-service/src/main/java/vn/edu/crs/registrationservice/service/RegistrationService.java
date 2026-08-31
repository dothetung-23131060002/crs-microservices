package vn.edu.crs.registrationservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.crs.registrationservice.client.CourseClient;
import vn.edu.crs.registrationservice.dto.RegistrationRequestDTO;
import vn.edu.crs.registrationservice.entity.Registration;
import vn.edu.crs.registrationservice.repository.RegistrationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    public static final String DA_DANG_KY = "DA_DANG_KY";
    public static final String DA_HUY = "DA_HUY";

    private final RegistrationRepository registrationRepository;
    private final CourseClient courseClient;

    public List<Registration> getMyRegistrations(Long studentId) {
        return registrationRepository.findByStudentId(studentId);
    }

    @Transactional
    public Registration register(RegistrationRequestDTO dto) {
        boolean alreadyRegistered = registrationRepository.existsByStudentIdAndCourseIdAndTrangThai(
                dto.getStudentId(), dto.getCourseId(), DA_DANG_KY);
        if (alreadyRegistered) {
            throw new IllegalStateException("Sinh vien da dang ky mon hoc nay roi");
        }

        // The remote reservation must succeed before any Registration row is inserted.
        courseClient.reserveSeat(dto.getCourseId());

        Registration registration = Registration.builder()
                .studentId(dto.getStudentId())
                .courseId(dto.getCourseId())
                .trangThai(DA_DANG_KY)
                .ngayDangKy(LocalDateTime.now())
                .build();
        return registrationRepository.save(registration);
    }

    @Transactional
    public void cancel(Long registrationId) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Khong tim thay dang ky id = " + registrationId));

        if (DA_HUY.equals(registration.getTrangThai())) {
            throw new IllegalStateException("Dang ky nay da duoc huy truoc do");
        }

        // Release the seat first; only a successful response may change local state.
        courseClient.releaseSeat(registration.getCourseId());
        registration.setTrangThai(DA_HUY);
        registrationRepository.save(registration);
    }
}
