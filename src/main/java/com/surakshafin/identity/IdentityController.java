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

    @GetMapping("/users/me")
    public ApiResponse<Dtos.UserProfile> me() {
        return ApiResponse.ok(identityService.getProfile(CurrentUser.id()));
    }
}
