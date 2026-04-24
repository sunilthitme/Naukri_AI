package com.aijobapplyassistant.dto.auth;

import com.aijobapplyassistant.dto.profile.UserProfileResponse;
import java.time.LocalDateTime;

public record AuthResponse(
        String token,
        LocalDateTime expiresAt,
        UserProfileResponse user
) {
}
