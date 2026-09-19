package com.surakshafin.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class Dtos {
    // grouped request/response records for the identity module

    public record RegisterRequest(
            @NotBlank @Pattern(regexp = "^[6-9]\\d{9}$", message = "must be a valid 10-digit Indian mobile number") String phoneNumber,
            @NotBlank @Size(max = 120) String fullName,
            // Security gap fix: previously only @NotBlank, so "a" was a valid password.
            @NotBlank
            @Size(min = 8, max = 72, message = "password must be at least 8 characters")
            @Pattern(
                    regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                    message = "password must contain at least one letter and one number"
            )
            String password,
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
            boolean kycLiteVerified,
            boolean admin
    ) {
        public static UserProfile from(User u) {
            return new UserProfile(u.getId(), u.getPhoneNumber(), u.getFullName(),
                    u.getPreferredLanguage(), u.isKycLiteVerified(), u.isAdmin());
        }
    }

    // --- feature gap fix: password reset (previously no recovery path existed) ---

    public record ForgotPasswordRequest(@NotBlank String phoneNumber) {}

    /**
     * Demo-mode response: since there's no SMS/OTP gateway wired up yet, the reset token is
     * returned directly instead of being texted to the user. In production this method returns
     * only a generic acknowledgement and the token is delivered exclusively via the SMS gateway.
     */
    public record ForgotPasswordResponse(String message, String demoResetToken) {}

    public record ResetPasswordRequest(
            @NotBlank String phoneNumber,
            @NotBlank String resetToken,
            @NotBlank
            @Size(min = 8, max = 72, message = "password must be at least 8 characters")
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "password must contain at least one letter and one number")
            String newPassword
    ) {}

    // --- feature gap fix: KYC-lite verification (the flag existed but nothing ever set it) ---

    public record KycLiteVerifyRequest(
            @NotBlank @Pattern(regexp = "^[A-Za-z]{5}\\d{4}[A-Za-z]$", message = "must be a valid PAN format") String panNumber
    ) {}
}
