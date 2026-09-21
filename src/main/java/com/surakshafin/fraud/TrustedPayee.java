package com.surakshafin.fraud;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

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

    @Column(nullable = false)
    private String payeeIdentifier;

    private String label;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
