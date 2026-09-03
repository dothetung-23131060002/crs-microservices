package vn.edu.crs.authservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.crs.authservice.dto.ApiKeyCreateRequestDTO;
import vn.edu.crs.authservice.dto.ApiKeyResponseDTO;
import vn.edu.crs.authservice.security.JwtUtil;
import vn.edu.crs.authservice.service.ApiKeyService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiKeyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ApiKeyService apiKeyService;

    @Test
    void unauthenticatedCannotAccessApiKeys() throws Exception {
        mockMvc.perform(get("/api-keys"))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotAccessApiKeys() throws Exception {
        String studentToken = jwtUtil.generateToken(2L, "student1", "STUDENT");

        mockMvc.perform(get("/api-keys")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateAndListAndRevokeApiKey() throws Exception {
        String adminToken = jwtUtil.generateToken(1L, "admin", "ADMIN");

        // 1. Create API key
        mockMvc.perform(post("/api-keys")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ownerName": "Doi tac Test",
                                  "scopes": "courses:read",
                                  "validDays": 30
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.keyValue").value(org.hamcrest.Matchers.startsWith("crs_")))
                .andExpect(jsonPath("$.ownerName").value("Doi tac Test"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // 2. List API keys
        mockMvc.perform(get("/api-keys")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void internalValidateEndpointIsAccessibleWithoutAuth() throws Exception {
        ApiKeyCreateRequestDTO dto = new ApiKeyCreateRequestDTO();
        dto.setOwnerName("Internal Partner");
        dto.setScopes("courses:read");
        dto.setValidDays(10);
        ApiKeyResponseDTO created = apiKeyService.create(dto);

        // Internal validate does not require JWT
        mockMvc.perform(get("/internal/api-keys/validate")
                        .param("key", created.getKeyValue())
                        .param("scope", "courses:read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        mockMvc.perform(get("/internal/api-keys/validate")
                        .param("key", created.getKeyValue())
                        .param("scope", "other:scope"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }
}
