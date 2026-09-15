package com.surakshafin.fraud;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Reference library of known scam patterns, surfaced to users as real-time alerts/awareness content. */
@Entity
@Table(name = "scam_patterns")
@Getter
@Setter
@NoArgsConstructor
public class ScamPattern {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false)
    private String category; // e.g. UPI_QR, FAKE_CUSTOMER_CARE, LOAN_APP, JOB_SCAM, KYC_PHISHING

    @Column(nullable = false)
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
}
