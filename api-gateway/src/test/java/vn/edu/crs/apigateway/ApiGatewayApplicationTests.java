package vn.edu.crs.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiGatewayApplicationTests {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void configuredRoutesAreLoaded() {
        StepVerifier.create(routeLocator.getRoutes().map(route -> route.getId()).collectList())
                .expectNextMatches(routeIds -> routeIds.contains("auth-service")
                        && routeIds.contains("course-service-list")
                        && routeIds.contains("course-service-detail")
                        && routeIds.contains("registration-service-list")
                        && routeIds.contains("registration-service-detail")
                        && routeIds.contains("course-service-partner"))
                .verifyComplete();
    }
}
