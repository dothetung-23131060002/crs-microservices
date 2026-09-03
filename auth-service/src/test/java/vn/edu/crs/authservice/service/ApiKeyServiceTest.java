package vn.edu.crs.authservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.crs.authservice.dto.ApiKeyCreateRequestDTO;
import vn.edu.crs.authservice.dto.ApiKeyResponseDTO;
import vn.edu.crs.authservice.entity.ApiKey;
import vn.edu.crs.authservice.repository.ApiKeyRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    private ApiKeyService apiKeyService;

    @BeforeEach
    void setUp() {
        apiKeyService = new ApiKeyService(apiKeyRepository);
    }

    @Test
    void createGeneratesSecureKeyWithCrsPrefix() {
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> {
            ApiKey k = invocation.getArgument(0);
            k.setId(1L);
            return k;
        });

        ApiKeyCreateRequestDTO dto = new ApiKeyCreateRequestDTO();
        dto.setOwnerName("Doi tac Test");
        dto.setScopes("courses:read");
        dto.setValidDays(30);

        ApiKeyResponseDTO response = apiKeyService.create(dto);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getKeyValue()).startsWith("crs_");
        assertThat(response.getOwnerName()).isEqualTo("Doi tac Test");
        assertThat(response.getScopes()).isEqualTo("courses:read");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getExpiresAt()).isNotNull();
    }

    @Test
    void createWithNullValidDaysHasNullExpiresAt() {
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> {
            ApiKey k = invocation.getArgument(0);
            k.setId(2L);
            return k;
        });

        ApiKeyCreateRequestDTO dto = new ApiKeyCreateRequestDTO();
        dto.setOwnerName("Permanent Partner");
        dto.setScopes("courses:read");
        dto.setValidDays(null);

        ApiKeyResponseDTO response = apiKeyService.create(dto);

        assertThat(response.getExpiresAt()).isNull();
    }

    @Test
    void revokeChangesStatusToRevoked() {
        ApiKey apiKey = new ApiKey(1L, "crs_key123", "Partner", "courses:read", "ACTIVE", null, LocalDateTime.now());
        when(apiKeyRepository.findById(1L)).thenReturn(Optional.of(apiKey));

        apiKeyService.revoke(1L);

        assertThat(apiKey.getStatus()).isEqualTo("REVOKED");
        verify(apiKeyRepository).save(apiKey);
    }

    @Test
    void revokeThrowsExceptionWhenNotFound() {
        when(apiKeyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> apiKeyService.revoke(99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void isValidForScopeChecksStatusAndExpiryAndScope() {
        ApiKey validKey = new ApiKey(1L, "crs_valid", "Partner", "courses:read,courses:write", "ACTIVE",
                LocalDateTime.now().plusDays(1), LocalDateTime.now());
        ApiKey expiredKey = new ApiKey(2L, "crs_expired", "Partner", "courses:read", "ACTIVE",
                LocalDateTime.now().minusDays(1), LocalDateTime.now());
        ApiKey revokedKey = new ApiKey(3L, "crs_revoked", "Partner", "courses:read", "REVOKED",
                LocalDateTime.now().plusDays(1), LocalDateTime.now());

        when(apiKeyRepository.findByKeyValue("crs_valid")).thenReturn(Optional.of(validKey));
        when(apiKeyRepository.findByKeyValue("crs_expired")).thenReturn(Optional.of(expiredKey));
        when(apiKeyRepository.findByKeyValue("crs_revoked")).thenReturn(Optional.of(revokedKey));
        when(apiKeyRepository.findByKeyValue("crs_unknown")).thenReturn(Optional.empty());

        assertThat(apiKeyService.isValidForScope("crs_valid", "courses:read")).isTrue();
        assertThat(apiKeyService.isValidForScope("crs_valid", "admin:all")).isFalse();
        assertThat(apiKeyService.isValidForScope("crs_expired", "courses:read")).isFalse();
        assertThat(apiKeyService.isValidForScope("crs_revoked", "courses:read")).isFalse();
        assertThat(apiKeyService.isValidForScope("crs_unknown", "courses:read")).isFalse();
    }
}
