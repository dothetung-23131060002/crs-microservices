// path: registration-service/src/main/java/vn/edu/crs/registrationservice/service/RegistrationService.java
// purpose: logic nghiep vu dang ky/huy dang ky, phoi hop goi sang course-service truoc khi luu DB

package vn.edu.crs.registrationservice.service;

import vn.edu.crs.registrationservice.client.CourseClient;
import vn.edu.crs.registrationservice.dto.RegistrationRequestDTO;
import vn.edu.crs.registrationservice.entity.Registration;
import vn.edu.crs.registrationservice.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private static final String DA_DANG_KY = "DA_DANG_KY";
    private static final String DA_HUY = "DA_HUY";

    private final RegistrationRepository registrationRepository;
    private final CourseClient courseClient;

    public Registration register(RegistrationRequestDTO dto) {
        if (registrationRepository.existsByStudentIdAndCourseIdAndTrangThai(
                dto.getStudentId(), dto.getCourseId(), DA_DANG_KY)) {
            throw new IllegalStateException("Sinh vien da dang ky mon hoc nay roi");
        }

        // Buoc 1: goi sang course-service de tru cho TRUOC.
        // Neu buoc nay nem exception, ham se dung lai ngay, KHONG luu Registration.
        courseClient.reserveSeat(dto.getCourseId());

        // Buoc 2: chi luu Registration SAU KHI course-service xac nhan thanh cong.
        Registration registration = new Registration();
        registration.setStudentId(dto.getStudentId());
        registration.setCourseId(dto.getCourseId());
        registration.setTrangThai(DA_DANG_KY);
        registration.setNgayDangKy(LocalDateTime.now());
        return registrationRepository.save(registration);

        // LUU Y (thao luan o muc D): neu dong save() nay that bai (vi du mat ket noi DB dung
        // luc nay), cho da bi tru o course-service nhung khong co ban ghi Registration nao duoc
        // tao. Day la gioi han da biet cua kien truc don gian hoa nay.
    }

    public void cancel(Long registrationId) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new NoSuchElementException("Khong tim thay dang ky id = " + registrationId));

        if (DA_HUY.equals(registration.getTrangThai())) {
            throw new IllegalStateException("Dang ky nay da duoc huy truoc do");
        }

        // Goi sang course-service de hoan tra cho TRUOC khi doi trang thai
        courseClient.releaseSeat(registration.getCourseId());

        registration.setTrangThai(DA_HUY);
        registrationRepository.save(registration);
    }
}
