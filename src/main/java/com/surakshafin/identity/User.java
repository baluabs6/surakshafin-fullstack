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

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
