package com.surakshafin.fraud;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FraudReportRepository extends JpaRepository<FraudReport, Long> {
    List<FraudReport> findByReportedByUserIdOrderByCreatedAtDesc(Long userId);

    long countBySuspectUpiId(String suspectUpiId);
    long countBySuspectPhoneNumber(String suspectPhoneNumber);
}
