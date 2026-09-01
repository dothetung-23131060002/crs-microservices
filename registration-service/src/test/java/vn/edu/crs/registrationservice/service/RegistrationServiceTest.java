package vn.edu.crs.registrationservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.crs.registrationservice.client.CourseClient;
import vn.edu.crs.registrationservice.dto.RegistrationRequestDTO;
import vn.edu.crs.registrationservice.entity.Registration;
import vn.edu.crs.registrationservice.repository.RegistrationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private CourseClient courseClient;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void getMyRegistrationsDelegatesToRepositoryWithStudentId() {
        Registration registration = activeRegistration();
        when(registrationRepository.findByStudentId(7L)).thenReturn(List.of(registration));

        assertThat(registrationService.getMyRegistrations(7L)).containsExactly(registration);

        verify(registrationRepository).findByStudentId(7L);
    }

    @Test
    void registerReservesSeatBeforeSavingRegistration() {
        RegistrationRequestDTO request = new RegistrationRequestDTO(7L, 11L);
        when(registrationRepository.existsByStudentIdAndCourseIdAndTrangThai(
                7L, 11L, RegistrationService.DA_DANG_KY)).thenReturn(false);
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> {
            Registration saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        Registration result = registrationService.register(request);

        InOrder order = inOrder(registrationRepository, courseClient);
        order.verify(registrationRepository).existsByStudentIdAndCourseIdAndTrangThai(
                7L, 11L, RegistrationService.DA_DANG_KY);
        order.verify(courseClient).reserveSeat(11L);
        order.verify(registrationRepository).save(any(Registration.class));
        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getStudentId()).isEqualTo(7L);
        assertThat(result.getCourseId()).isEqualTo(11L);
        assertThat(result.getTrangThai()).isEqualTo(RegistrationService.DA_DANG_KY);
        assertThat(result.getNgayDangKy()).isNotNull();
    }

    @Test
    void registerRejectsAnExistingActiveRegistrationWithoutReservingSeat() {
        RegistrationRequestDTO request = new RegistrationRequestDTO(7L, 11L);
        when(registrationRepository.existsByStudentIdAndCourseIdAndTrangThai(
                7L, 11L, RegistrationService.DA_DANG_KY)).thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Sinh vien da dang ky mon hoc nay roi");

        verifyNoInteractions(courseClient);
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void registerDoesNotSaveWhenCourseServiceRejectsReservation() {
        RegistrationRequestDTO request = new RegistrationRequestDTO(7L, 11L);
        when(registrationRepository.existsByStudentIdAndCourseIdAndTrangThai(
                7L, 11L, RegistrationService.DA_DANG_KY)).thenReturn(false);
        doThrow(new IllegalStateException("Mon hoc da het cho"))
                .when(courseClient).reserveSeat(11L);

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Mon hoc da het cho");

        verify(registrationRepository, never()).save(any());
    }

    @Test
    void cancelReleasesSeatBeforeChangingStatusAndSaving() {
        Registration registration = activeRegistration();
        when(registrationRepository.findById(23L)).thenReturn(Optional.of(registration));
        doAnswer(invocation -> {
            assertThat(registration.getTrangThai()).isEqualTo(RegistrationService.DA_DANG_KY);
            return null;
        }).when(courseClient).releaseSeat(11L);

        registrationService.cancel(23L);

        InOrder order = inOrder(registrationRepository, courseClient);
        order.verify(registrationRepository).findById(23L);
        order.verify(courseClient).releaseSeat(11L);
        order.verify(registrationRepository).save(registration);
        assertThat(registration.getTrangThai()).isEqualTo(RegistrationService.DA_HUY);
    }

    @Test
    void cancelKeepsActiveStateWhenCourseServiceIsUnavailable() {
        Registration registration = activeRegistration();
        when(registrationRepository.findById(23L)).thenReturn(Optional.of(registration));
        doThrow(new IllegalStateException("unavailable", new RuntimeException()))
                .when(courseClient).releaseSeat(11L);

        assertThatThrownBy(() -> registrationService.cancel(23L))
                .isInstanceOf(IllegalStateException.class);

        assertThat(registration.getTrangThai()).isEqualTo(RegistrationService.DA_DANG_KY);
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void cancelRejectsAnAlreadyCancelledRegistrationWithoutReleasingAgain() {
        Registration registration = activeRegistration();
        registration.setTrangThai(RegistrationService.DA_HUY);
        when(registrationRepository.findById(23L)).thenReturn(Optional.of(registration));

        assertThatThrownBy(() -> registrationService.cancel(23L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Dang ky nay da duoc huy truoc do");

        verifyNoInteractions(courseClient);
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void cancelReturnsNotFoundBeforeCallingCourseService() {
        when(registrationRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.cancel(404L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Khong tim thay dang ky id = 404");

        verifyNoInteractions(courseClient);
    }

    private Registration activeRegistration() {
        return Registration.builder()
                .id(23L)
                .studentId(7L)
                .courseId(11L)
                .trangThai(RegistrationService.DA_DANG_KY)
                .ngayDangKy(LocalDateTime.of(2026, 8, 16, 9, 0))
                .build();
    }
}
