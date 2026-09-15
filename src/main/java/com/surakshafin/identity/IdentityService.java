package com.surakshafin.identity;

import com.surakshafin.common.BadRequestException;
import com.surakshafin.common.NotFoundException;
import com.surakshafin.config.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class IdentityService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public IdentityService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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
        User user = userRepository.findByPhoneNumber(req.phoneNumber())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return buildAuthResponse(user);
    }

    public Dtos.UserProfile getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return Dtos.UserProfile.from(user);
    }

    private Dtos.AuthResponse buildAuthResponse(User user) {
        String token = jwtService.issueToken(user.getId(), user.getPhoneNumber());
        return new Dtos.AuthResponse(token, jwtService.expirationSeconds(), Dtos.UserProfile.from(user));
    }
}
