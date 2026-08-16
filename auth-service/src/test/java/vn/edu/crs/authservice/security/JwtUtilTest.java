package vn.edu.crs.authservice.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

class JwtUtilTest {

    private static final String SECRET =
            "CRS-Microservices-Secret-Key-Nam-3-Hoc-Ky-2026-Doi-Trong-Thuc-Te";

    @Test
    void generatedTokenContainsExpectedClaimsAndExpiration() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000);

        String token = jwtUtil.generateToken("student1", "STUDENT");
        Claims claims = jwtUtil.parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo("student1");
        assertThat(claims.get("role", String.class)).isEqualTo("STUDENT");
        assertThat(claims.getIssuedAt()).isBeforeOrEqualTo(claims.getExpiration());
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime())
                .isEqualTo(60_000);
        assertThat(jwtUtil.isTokenValid(token)).isTrue();
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtUtil issuer = new JwtUtil(SECRET, 60_000);
        JwtUtil verifier = new JwtUtil(
                "Another-CRS-Microservices-Secret-Key-With-At-Least-32-Bytes", 60_000);

        String token = issuer.generateToken("admin", "ADMIN");

        assertThat(verifier.isTokenValid(token)).isFalse();
    }
}
