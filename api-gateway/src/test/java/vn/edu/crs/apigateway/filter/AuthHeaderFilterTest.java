package vn.edu.crs.apigateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class AuthHeaderFilterTest {

    private final AuthHeaderFilter filter = new AuthHeaderFilter();

    @Test
    void publicCourseReadPassesWithoutAuthorization() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/courses").build());
        AtomicBoolean called = new AtomicBoolean();
        GatewayFilterChain chain = ignored -> {
            called.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(called).isTrue();
    }

    @Test
    void protectedWriteWithoutBearerTokenIsRejected() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/courses").build());

        StepVerifier.create(filter.filter(exchange, ignored -> Mono.empty())).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedWriteWithBearerTokenPasses() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/courses")
                        .header("Authorization", "Bearer token")
                        .build());
        AtomicBoolean called = new AtomicBoolean();

        StepVerifier.create(filter.filter(exchange, ignored -> {
            called.set(true);
            return Mono.empty();
        })).verifyComplete();

        assertThat(called).isTrue();
    }
}
