package com.surakshafin.budget;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserIdOrderByOccurredAtDesc(Long userId);

    /** Transactions in the half-open window [from, to) — used to scope the budget to one month. */
    List<Transaction> findByUserIdAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtDesc(
            Long userId, Instant from, Instant to);
}
