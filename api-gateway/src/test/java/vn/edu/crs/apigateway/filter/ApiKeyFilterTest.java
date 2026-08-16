package vn.edu.crs.apigateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyFilterTest {

    private final ApiKeyFilter filter = new ApiKeyFilter("partner-secret");

    @Test
    void partnerRouteRejectsMissingApiKey() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/public/courses").build());

        StepVerifier.create(filter.filter(exchange, ignored -> Mono.empty())).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void partnerRouteAcceptsCorrectApiKey() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/public/courses")
                        .header("X-API-KEY", "partner-secret")
                        .build());
        AtomicBoolean called = new AtomicBoolean();

        StepVerifier.create(filter.filter(exchange, ignored -> {
            called.set(true);
            return Mono.empty();
        })).verifyComplete();

        assertThat(called).isTrue();
    }
}
