package vn.edu.crs.apigateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import vn.edu.crs.apigateway.cache.ApiKeyValidationCache;
import vn.edu.crs.apigateway.client.AuthServiceClient;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyFilterTest {

    @Mock
    private AuthServiceClient authServiceClient;

    private ApiKeyValidationCache cache;
    private ApiKeyFilter filter;

    @BeforeEach
    void setUp() {
        cache = new ApiKeyValidationCache();
        filter = new ApiKeyFilter(authServiceClient, cache);
    }

    @Test
    void nonPartnerRoutePassesThroughWithoutCheck() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/courses").build());
        AtomicBoolean called = new AtomicBoolean();

        StepVerifier.create(filter.filter(exchange, ignored -> {
            called.set(true);
            return Mono.empty();
        })).verifyComplete();

        assertThat(called).isTrue();
        verifyNoInteractions(authServiceClient);
    }

    @Test
    void partnerRouteRejectsMissingApiKey() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/public/courses").build());

        StepVerifier.create(filter.filter(exchange, ignored -> Mono.empty())).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(authServiceClient);
    }

    @Test
    void partnerRouteRejectsBlankApiKey() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/public/courses")
                        .header("X-API-KEY", "   ")
                        .build());

        StepVerifier.create(filter.filter(exchange, ignored -> Mono.empty())).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(authServiceClient);
    }

    @Test
    void partnerRouteAcceptsValidApiKeyAndUsesCache() {
        when(authServiceClient.isValidForScope(eq("crs_test_key"), eq("courses:read")))
                .thenReturn(Mono.just(true));

        MockServerWebExchange exchange1 = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/public/courses")
                        .header("X-API-KEY", "crs_test_key")
                        .build());
        AtomicBoolean called1 = new AtomicBoolean();

        StepVerifier.create(filter.filter(exchange1, ignored -> {
            called1.set(true);
            return Mono.empty();
        })).verifyComplete();

        assertThat(called1).isTrue();
        verify(authServiceClient, times(1)).isValidForScope("crs_test_key", "courses:read");

        // Second call should hit cache, NOT authServiceClient again
        MockServerWebExchange exchange2 = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/public/courses")
                        .header("X-API-KEY", "crs_test_key")
                        .build());
        AtomicBoolean called2 = new AtomicBoolean();

        StepVerifier.create(filter.filter(exchange2, ignored -> {
            called2.set(true);
            return Mono.empty();
        })).verifyComplete();

        assertThat(called2).isTrue();
        verify(authServiceClient, times(1)).isValidForScope("crs_test_key", "courses:read");
    }

    @Test
    void partnerRouteRejectsInvalidApiKeyAndCachesRejection() {
        when(authServiceClient.isValidForScope(eq("crs_invalid_key"), eq("courses:read")))
                .thenReturn(Mono.just(false));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/public/courses")
                        .header("X-API-KEY", "crs_invalid_key")
                        .build());

        StepVerifier.create(filter.filter(exchange, ignored -> Mono.empty())).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
