package com.surakshafin.fraud;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** A user's one-tap scam/fraud report. In production this fans out a FraudAlertRaised event
 *  to notification-service and audit-compliance-service, and forwards to NPCI/Cyber Crime Portal. */
@Entity
@Table(name = "fraud_reports")
@Getter
@Setter
@NoArgsConstructor
public class FraudReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long reportedByUserId;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false, length = 2000)
    private String details;

    private String suspectUpiId;
    private String suspectPhoneNumber;

    @Column(nullable = false)
    private String status = "SUBMITTED"; // SUBMITTED, FORWARDED_TO_NPCI, FORWARDED_TO_CYBER_CELL, RESOLVED

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
