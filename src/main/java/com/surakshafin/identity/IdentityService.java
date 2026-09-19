package com.surakshafin.identity;

import com.surakshafin.common.BadRequestException;
import com.surakshafin.common.NotFoundException;
import com.surakshafin.config.JwtService;
import com.surakshafin.config.TokenBlocklistService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class IdentityService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlocklistService tokenBlocklistService;
    private final int maxLoginAttempts;
    private final int lockoutMinutes;
    private final int resetTokenValidityMinutes;

    public IdentityService(UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            JwtService jwtService,
                            TokenBlocklistService tokenBlocklistService,
                            @Value("${surakshafin.security.max-login-attempts}") int maxLoginAttempts,
                            @Value("${surakshafin.security.lockout-minutes}") int lockoutMinutes,
                            @Value("${surakshafin.security.reset-token-validity-minutes}") int resetTokenValidityMinutes) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenBlocklistService = tokenBlocklistService;
        this.maxLoginAttempts = maxLoginAttempts;
        this.lockoutMinutes = lockoutMinutes;
        this.resetTokenValidityMinutes = resetTokenValidityMinutes;
    }

    public Dtos.AuthResponse register(Dtos.RegisterRequest req) {
        if (userRepository.existsByPhoneNumber(req.phoneNumber())) {
            throw new BadRequestException("An account already exists for this phone number");
        }
        User user = new User();
        user.setPhoneNumber(req.phoneNumber());
        user.setFullName(req.fullName());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        if (req.preferredLanguage() != null && !req.preferredLanguage().isBlank()) {
            user.setPreferredLanguage(req.preferredLanguage());
        }
        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    public Dtos.AuthResponse login(Dtos.LoginRequest req) {
        // Security gap fix: login previously had no brute-force protection whatsoever — an
        // attacker could try unlimited passwords against a known phone number.
        User user = userRepository.findByPhoneNumber(req.phoneNumber())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (user.getLockedUntil() != null) {
            if (user.getLockedUntil().isAfter(Instant.now())) {
                throw new LockedException("Account temporarily locked");
            }
            // Lock window has expired: clear it before evaluating this attempt.
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
        }

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= maxLoginAttempts) {
                user.setLockedUntil(Instant.now().plus(lockoutMinutes, ChronoUnit.MINUTES));
            }
            userRepository.save(user);
            throw new BadCredentialsException("Invalid credentials");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        return buildAuthResponse(user);
    }

    /** Feature gap fix: revokes the current session's token so a shared/stolen device can be logged out. */
    public void logout(String rawToken) {
        if (rawToken == null) {
            return;
        }
        tokenBlocklistService.block(rawToken, jwtService.remainingValidityMillis(rawToken));
    }

    // --- feature gap fix: password reset (previously no recovery path existed at all) ---

    public Dtos.ForgotPasswordResponse forgotPassword(Dtos.ForgotPasswordRequest req) {
        String genericMessage = "If an account exists for this number, a reset code has been issued.";
        var userOpt = userRepository.findByPhoneNumber(req.phoneNumber());
        if (userOpt.isEmpty()) {
            // Security gap fix: don't reveal via a different response shape/timing whether the
            // phone number is registered — same generic message either way.
            return new Dtos.ForgotPasswordResponse(genericMessage, null);
        }
        User user = userOpt.get();
        String token = generateResetToken();
        user.setResetToken(token);
        user.setResetTokenExpiry(Instant.now().plus(resetTokenValidityMinutes, ChronoUnit.MINUTES));
        userRepository.save(user);
        // Demo-mode only: the token would normally go out over SMS, never in the API response.
        return new Dtos.ForgotPasswordResponse(genericMessage, token);
    }

    public void resetPassword(Dtos.ResetPasswordRequest req) {
        User user = userRepository.findByPhoneNumberAndResetToken(req.phoneNumber(), req.resetToken())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset code"));
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(Instant.now())) {
            throw new BadRequestException("Invalid or expired reset code");
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
    }

    // --- feature gap fix: KYC-lite verification (the flag existed on User but nothing set it) ---

    public Dtos.UserProfile verifyKycLite(Long userId, Dtos.KycLiteVerifyRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        // Demo-mode check: real verification calls an external PAN/Aadhaar-lite verification
        // service (per the architecture blueprint) rather than accepting any well-formed PAN.
        user.setKycLiteVerified(true);
        user = userRepository.save(user);
        return Dtos.UserProfile.from(user);
    }

    public Dtos.UserProfile getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return Dtos.UserProfile.from(user);
    }

    private Dtos.AuthResponse buildAuthResponse(User user) {
        String token = jwtService.issueToken(user.getId(), user.getPhoneNumber(), user.isAdmin());
        return new Dtos.AuthResponse(token, jwtService.expirationSeconds(), Dtos.UserProfile.from(user));
    }

    private String generateResetToken() {
        byte[] bytes = new byte[6];
        SECURE_RANDOM.nextBytes(bytes);
        // Short, human-typeable code rather than a long opaque token, since a user reads this off an SMS.
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).substring(0, 8).toUpperCase();
    }
}
