// path: auth-service/src/main/java/vn/edu/crs/authservice/controller/InternalApiKeyController.java
// purpose: endpoint noi bo de api-gateway kiem tra 1 API Key co hop le cho 1 scope khong
package vn.edu.crs.authservice.controller;

import vn.edu.crs.authservice.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal/api-keys")
@RequiredArgsConstructor
public class InternalApiKeyController {

    private final ApiKeyService apiKeyService;

    @GetMapping("/validate")
    public Map<String, Object> validate(
            @RequestParam String key,
            @RequestParam String scope) {
        boolean valid = apiKeyService.isValidForScope(key, scope);
        return Map.of("valid", valid);
    }
}
