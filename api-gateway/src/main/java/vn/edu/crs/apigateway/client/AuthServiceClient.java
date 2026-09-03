// path: api-gateway/src/main/java/vn/edu/crs/apigateway/client/AuthServiceClient.java
// purpose: goi sang auth-service (endpoint noi bo) de kiem tra API Key, dung WebClient
// vi api-gateway chay tren nen reactive (WebFlux)
package vn.edu.crs.apigateway.client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class AuthServiceClient {

    private final WebClient webClient;

    public AuthServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8081").build();
    }

    public Mono<Boolean> isValidForScope(String key, String scope) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/api-keys/validate")
                        .queryParam("key", key)
                        .queryParam("scope", scope)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .map(res -> Boolean.TRUE.equals(res.get("valid")))
                .onErrorReturn(false); // neu auth-service khong ket noi duoc, coi nhu key khong hop le (fail-safe)
    }
}
