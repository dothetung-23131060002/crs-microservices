package vn.edu.crs.registrationservice;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.crs.registrationservice.client.CourseClient;
import vn.edu.crs.registrationservice.entity.Registration;
import vn.edu.crs.registrationservice.repository.RegistrationRepository;
import vn.edu.crs.registrationservice.service.RegistrationService;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RegistrationApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegistrationRepository registrationRepository;

    @MockBean
    private CourseClient courseClient;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Test
    void protectedEndpointRejectsRequestWithoutJwtAsJson() throws Exception {
        mockMvc.perform(post("/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":1,\"courseId\":1}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));

        assertThat(registrationRepository.count()).isZero();
        verify(courseClient, never()).reserveSeat(1L);
    }

    @Test
    void protectedEndpointRejectsInvalidJwtAsJson() throws Exception {
        mockMvc.perform(post("/registrations")
                        .header("Authorization", "Bearer not-a-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":1,\"courseId\":1}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void validJwtCanCreateRegistrationAfterRemoteReservation() throws Exception {
        mockMvc.perform(post("/registrations")
                        .header("Authorization", bearerToken(1L, "student1", "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":1,\"courseId\":9}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.studentId").value(1))
                .andExpect(jsonPath("$.courseId").value(9))
                .andExpect(jsonPath("$.trangThai").value(RegistrationService.DA_DANG_KY));

        verify(courseClient).reserveSeat(9L);
        assertThat(registrationRepository.findByStudentId(1L))
                .singleElement()
                .extracting(Registration::getTrangThai)
                .isEqualTo(RegistrationService.DA_DANG_KY);
    }

    @Test
    void getMyRegistrationsUsesStudentIdFromJwtCredentials() throws Exception {
        registrationRepository.save(Registration.builder()
                .studentId(1L)
                .courseId(9L)
                .trangThai(RegistrationService.DA_DANG_KY)
                .ngayDangKy(LocalDateTime.now())
                .build());
        registrationRepository.save(Registration.builder()
                .studentId(2L)
                .courseId(10L)
                .trangThai(RegistrationService.DA_DANG_KY)
                .ngayDangKy(LocalDateTime.now())
                .build());

        mockMvc.perform(get("/registrations/my")
                        .header("Authorization", bearerToken(1L, "student1", "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].studentId").value(1))
                .andExpect(jsonPath("$[0].courseId").value(9));
    }

    @Test
    void validationErrorIsReturnedAsJsonAndDoesNotReserveSeat() throws Exception {
        mockMvc.perform(post("/registrations")
                        .header("Authorization", bearerToken(1L, "student1", "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.courseId").value("courseId khong duoc de trong"));

        verify(courseClient, never()).reserveSeat(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void duplicateActiveRegistrationReturnsConflictWithoutSecondReservation() throws Exception {
        registrationRepository.save(Registration.builder()
                .studentId(1L)
                .courseId(9L)
                .trangThai(RegistrationService.DA_DANG_KY)
                .ngayDangKy(LocalDateTime.now())
                .build());

        mockMvc.perform(post("/registrations")
                        .header("Authorization", bearerToken(1L, "student1", "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":1,\"courseId\":9}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Sinh vien da dang ky mon hoc nay roi"));

        verify(courseClient, never()).reserveSeat(9L);
    }

    @Test
    void cancelReleasesSeatThenMarksRegistrationCancelled() throws Exception {
        Registration registration = registrationRepository.save(Registration.builder()
                .studentId(1L)
                .courseId(9L)
                .trangThai(RegistrationService.DA_DANG_KY)
                .ngayDangKy(LocalDateTime.now())
                .build());

        mockMvc.perform(delete("/registrations/{id}", registration.getId())
                        .header("Authorization", bearerToken(1L, "admin", "ADMIN")))
                .andExpect(status().isOk());

        verify(courseClient).releaseSeat(9L);
        assertThat(registrationRepository.findById(registration.getId()))
                .get()
                .extracting(Registration::getTrangThai)
                .isEqualTo(RegistrationService.DA_HUY);
    }

    private String bearerToken(Long userId, String username, String role) {
        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + 60_000L);
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("role", role)
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        return "Bearer " + token;
    }
}
