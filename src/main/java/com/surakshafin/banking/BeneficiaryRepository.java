package com.surakshafin.banking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {
    List<Beneficiary> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Beneficiary> findByUserIdAndAccountNumber(Long userId, String accountNumber);
    Optional<Beneficiary> findByIdAndUserId(Long id, Long userId);
}
