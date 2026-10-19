package com.surakshafin.banking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankTransferRepository extends JpaRepository<BankTransfer, Long> {
    Optional<BankTransfer> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);
    List<BankTransfer> findTop50ByUserIdOrderByCreatedAtDescIdDesc(Long userId);
}
