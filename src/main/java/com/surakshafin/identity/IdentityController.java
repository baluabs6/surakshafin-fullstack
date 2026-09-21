package com.surakshafin.identity;

import com.surakshafin.common.ApiResponse;
import com.surakshafin.config.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class IdentityController {

    private final IdentityService identityService;

    public IdentityController(IdentityService identityService) {
        this.identityService = identityService;
    }

    @PostMapping("/auth/register")
    public ApiResponse<Dtos.AuthResponse> register(@Valid @RequestBody Dtos.RegisterRequest request) {
        return ApiResponse.ok(identityService.register(request), "Account created");
    }

    @PostMapping("/auth/login")
    public ApiResponse<Dtos.AuthResponse> login(@Valid @RequestBody Dtos.LoginRequest request) {
        return ApiResponse.ok(identityService.login(request), "Logged in");
    }

    @PostMapping("/auth/logout")
    public ApiResponse<Void> logout() {
        identityService.logout(CurrentUser.rawToken());
        return ApiResponse.ok(null, "Logged out");
    }

    @PostMapping("/auth/forgot-password")
    public ApiResponse<Dtos.ForgotPasswordResponse> forgotPassword(@Valid @RequestBody Dtos.ForgotPasswordRequest request) {
        return ApiResponse.ok(identityService.forgotPassword(request));
    }

    @PostMapping("/auth/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody Dtos.ResetPasswordRequest request) {
        identityService.resetPassword(request);
        return ApiResponse.ok(null, "Password updated. Please log in with your new password.");
    }

    @GetMapping("/users/me")
    public ApiResponse<Dtos.UserProfile> me() {
        return ApiResponse.ok(identityService.getProfile(CurrentUser.id()));
    }

    @PostMapping("/users/me/kyc-lite")
    public ApiResponse<Dtos.UserProfile> verifyKycLite(@Valid @RequestBody Dtos.KycLiteVerifyRequest request) {
        return ApiResponse.ok(identityService.verifyKycLite(CurrentUser.id(), request), "KYC-lite verified");
    }
}
