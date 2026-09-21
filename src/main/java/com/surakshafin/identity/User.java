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

    @Column(nullable = false)
    private String preferredLanguage = "en";

    @Column(nullable = false)
    private boolean kycLiteVerified = false;

    @Column(nullable = false)
    private boolean admin = false;

    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    private Instant lockedUntil;

    private String resetToken;
    private Instant resetTokenExpiry;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
