// path: api-gateway/src/main/java/vn/edu/crs/apigateway/config/WebClientConfig.java
// purpose: khai bao bean WebClient.Builder de AuthServiceClient su dung
package vn.edu.crs.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
