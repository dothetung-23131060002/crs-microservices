// path: registration-service/src/main/java/vn/edu/crs/registrationservice/config/RestTemplateConfig.java
// purpose: cau hinh RestTemplate dung JdkClientHttpRequestFactory (dua tren java.net.http.HttpClient)
// vi SimpleClientHttpRequestFactory mac dinh (dua tren HttpURLConnection) KHONG ho tro PATCH

package vn.edu.crs.registrationservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate(new JdkClientHttpRequestFactory());
    }
}
