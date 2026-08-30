package vn.edu.crs.courseservice.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.crs.courseservice.entity.Course;
import vn.edu.crs.courseservice.repository.CourseRepository;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CourseControllerIntegrationTest {

    private static final String TEST_SECRET =
            "CRS-Microservices-Test-Secret-Key-At-Least-32-Bytes-Long";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @BeforeEach
    void clearDatabase() {
        courseRepository.deleteAll();
    }

    @Test
    void publicSearchSupportsKeywordPaginationAndSort() throws Exception {
        courseRepository.save(course("Lap trinh Java", 3, 40, 40));
        courseRepository.save(course("Java nang cao", 4, 30, 25));
        courseRepository.save(course("Co so du lieu", 3, 50, 50));

        mockMvc.perform(get("/courses")
                        .param("keyword", "java")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "tenMonHoc,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].tenMonHoc").value("Java nang cao"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void writesRequireAdminAndValidationErrorsAreJson() throws Exception {
        String validBody = """
                {"tenMonHoc":"Kien truc phan mem","soTinChi":3,"soChoToiDa":30}
                """;

        mockMvc.perform(post("/courses")
                        .header("Authorization", "Bearer chuoi-rac-buoi-08")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/courses")
                        .header("Authorization", bearerToken("student1", "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/courses")
                        .header("Authorization", bearerToken("admin", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenMonHoc":"","soTinChi":0,"soChoToiDa":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.tenMonHoc").exists())
                .andExpect(jsonPath("$.soTinChi").exists())
                .andExpect(jsonPath("$.soChoToiDa").exists());

        mockMvc.perform(post("/courses")
                        .header("Authorization", bearerToken("admin", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.soChoConLai").value(30));

        mockMvc.perform(post("/courses")
                        .header("Authorization", bearerToken("admin", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenMonHoc":"kien truc phan mem","soTinChi":3,"soChoToiDa":30}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ten mon hoc da ton tai"));
    }

    @Test
    void adminCanUpdateWithoutResettingRemainingSeatsAndDelete() throws Exception {
        Course stored = courseRepository.save(course("Lap trinh Java", 3, 10, 4));
        String adminToken = bearerToken("admin", "ADMIN");

        mockMvc.perform(put("/courses/{id}", stored.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenMonHoc":"Lap trinh huong dich vu","soTinChi":4,"soChoToiDa":12,"soChoConLai":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenMonHoc").value("Lap trinh huong dich vu"))
                .andExpect(jsonPath("$.soChoConLai").value(4));

        Course updated = courseRepository.findById(stored.getId()).orElseThrow();
        assertThat(updated.getSoChoConLai()).isEqualTo(4);

        mockMvc.perform(delete("/courses/{id}", stored.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/courses/{id}", stored.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(
                        "Khong tim thay mon hoc id = " + stored.getId()));
    }

    @Test
    void internalReserveAndReleaseArePublicAndRespectSeatBounds() throws Exception {
        Course stored = courseRepository.save(course("He thong phan tan", 3, 1, 1));

        mockMvc.perform(patch("/internal/courses/{id}/reserve-seat", stored.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.soChoConLai").value(0));

        mockMvc.perform(patch("/internal/courses/{id}/reserve-seat", stored.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Mon hoc da het cho, khong the dang ky"));

        mockMvc.perform(patch("/internal/courses/{id}/release-seat", stored.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.soChoConLai").value(1));

        mockMvc.perform(patch("/internal/courses/{id}/release-seat", stored.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.soChoConLai").value(1));
    }

    private Course course(
            String tenMonHoc,
            int soTinChi,
            int soChoToiDa,
            int soChoConLai) {
        return Course.builder()
                .tenMonHoc(tenMonHoc)
                .soTinChi(soTinChi)
                .soChoToiDa(soChoToiDa)
                .soChoConLai(soChoConLai)
                .build();
    }

    private String bearerToken(String username, String role) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        long now = System.currentTimeMillis();
        String token = Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + 60_000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        return "Bearer " + token;
    }
}
