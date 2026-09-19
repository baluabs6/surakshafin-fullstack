package com.surakshafin.config;

import com.surakshafin.identity.User;
import com.surakshafin.identity.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Feature gap fix: fraud-report and grievance status updates need an operator/admin role, but
 * there was previously no admin concept and no way to create one. Rather than hardcoding an
 * admin phone/password in source (a security gap in itself), this only creates the bootstrap
 * admin when both env vars are explicitly set, and only once (it never overwrites an existing
 * admin's password on restart).
 */
@Component
@Order(1) // run before DemoDataSeeder for a predictable startup order
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String bootstrapPhone;
    private final String bootstrapPassword;

    public AdminSeeder(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        @Value("${surakshafin.admin.bootstrap-phone}") String bootstrapPhone,
                        @Value("${surakshafin.admin.bootstrap-password}") String bootstrapPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapPhone = bootstrapPhone;
        this.bootstrapPassword = bootstrapPassword;
    }

    @Override
    public void run(String... args) {
        if (bootstrapPhone == null || bootstrapPhone.isBlank() || bootstrapPassword == null || bootstrapPassword.isBlank()) {
            log.info("No SURAKSHAFIN_ADMIN_PHONE/SURAKSHAFIN_ADMIN_PASSWORD set — skipping admin bootstrap. " +
                    "Fraud-report and grievance status-update endpoints will be unreachable until an admin exists.");
            return;
        }
        if (userRepository.existsByPhoneNumber(bootstrapPhone)) {
            return; // already provisioned; never overwrite an existing account's password on restart
        }
        User admin = new User();
        admin.setPhoneNumber(bootstrapPhone);
        admin.setFullName("Platform Operator");
        admin.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
        admin.setAdmin(true);
        admin.setKycLiteVerified(true);
        userRepository.save(admin);
        log.info("Bootstrap admin account created for {}", bootstrapPhone);
    }
}
