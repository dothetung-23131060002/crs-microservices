package vn.edu.crs.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponseDTO {

    private final Long userId;
    private final String token;
    private final String username;
    private final String role;
}
