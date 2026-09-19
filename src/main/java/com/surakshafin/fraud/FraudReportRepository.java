package com.surakshafin.fraud;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FraudReportRepository extends JpaRepository<FraudReport, Long> {
    List<FraudReport> findByReportedByUserIdOrderByCreatedAtDesc(Long userId);

    // New feature: pre-transaction safety check — lets FraudService see whether a payee
    // identifier has already been reported as a suspect by anyone on the platform.
    long countBySuspectUpiId(String suspectUpiId);
    long countBySuspectPhoneNumber(String suspectPhoneNumber);
}
