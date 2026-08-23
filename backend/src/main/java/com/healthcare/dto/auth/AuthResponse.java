package com.healthcare.dto.auth;

import com.healthcare.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    @Builder.Default
    private String tokenType = "Bearer";
    private Long userId;
    private String name;
    private String email;
    private User.Role role;
    // profileId is doctorId or patientId depending on role
    private Long profileId;
}
