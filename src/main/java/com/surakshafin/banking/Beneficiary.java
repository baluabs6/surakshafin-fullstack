package com.surakshafin.banking;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "beneficiaries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "accountNumber"}))
@Getter
@Setter
@NoArgsConstructor
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 16)
    private String accountNumber;

    @Column(nullable = false, length = 11)
    private String ifsc;

    /** Holder name, masked at the time the beneficiary was verified (e.g. "B***** A"). */
    @Column(nullable = false)
    private String maskedName;

    private String nickname;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
