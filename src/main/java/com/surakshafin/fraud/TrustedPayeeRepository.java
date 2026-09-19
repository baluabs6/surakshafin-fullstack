package com.surakshafin.fraud;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrustedPayeeRepository extends JpaRepository<TrustedPayee, Long> {
    List<TrustedPayee> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<TrustedPayee> findByUserIdAndPayeeIdentifier(Long userId, String payeeIdentifier);
    void deleteByUserIdAndId(Long userId, Long id);
}
