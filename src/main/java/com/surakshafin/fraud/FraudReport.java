package com.surakshafin.fraud;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

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
    private String status = "SUBMITTED";

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
