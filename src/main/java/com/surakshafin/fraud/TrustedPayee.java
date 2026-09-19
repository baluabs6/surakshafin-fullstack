package com.surakshafin.fraud;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** A payee the user has explicitly marked as trusted, so future transfers to them skip the
 *  pre-transaction safety friction. Never auto-populated — trust has to be an explicit user action. */
@Entity
@Table(name = "trusted_payees", uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "payeeIdentifier"}))
@Getter
@Setter
@NoArgsConstructor
public class TrustedPayee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /** A UPI ID or phone number, matched verbatim against pre-transaction-check requests. */
    @Column(nullable = false)
    private String payeeIdentifier;

    private String label; // e.g. "Landlord", "Mom" — optional, user-supplied

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
