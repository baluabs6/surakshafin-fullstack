package com.surakshafin.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class Dtos {
    // grouped request/response records for the identity module

    public record RegisterRequest(
            @NotBlank @Pattern(regexp = "^[6-9]\\d{9}$", message = "must be a valid 10-digit Indian mobile number") String phoneNumber,
            @NotBlank String fullName,
            @NotBlank String password,
            String preferredLanguage
    ) {}

    public record LoginRequest(
            @NotBlank String phoneNumber,
            @NotBlank String password
    ) {}

    public record AuthResponse(
            String token,
            long expiresInSeconds,
            UserProfile profile
    ) {}

    public record UserProfile(
            Long id,
            String phoneNumber,
            String fullName,
            String preferredLanguage,
            boolean kycLiteVerified
    ) {
        public static UserProfile from(User u) {
            return new UserProfile(u.getId(), u.getPhoneNumber(), u.getFullName(),
                    u.getPreferredLanguage(), u.isKycLiteVerified());
        }
    }
}
