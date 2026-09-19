package com.surakshafin.identity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "app_users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String phoneNumber;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String passwordHash;

    /** ISO 639-1 code: hi, te, ta, bn, mr, en ... drives literacy content + notification language. */
    @Column(nullable = false)
    private String preferredLanguage = "en";

    @Column(nullable = false)
    private boolean kycLiteVerified = false;

    /** True only for platform operators (fraud-cell / grievance-desk reviewers), never set from self-registration. */
    @Column(nullable = false)
    private boolean admin = false;

    // --- brute-force protection (security gap fix) ---
    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    /** Null when the account isn't locked; a future instant while a lockout is active. */
    private Instant lockedUntil;

    // --- password reset (feature gap fix) ---
    private String resetToken;
    private Instant resetTokenExpiry;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
