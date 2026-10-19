package com.surakshafin.banking;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FixedDepositRepository extends JpaRepository<FixedDeposit, Long> {
    List<FixedDeposit> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);
    List<FixedDeposit> findByStatusAndMaturityDateLessThanEqual(FdStatus status, LocalDate date);

    /** Lock order is always FD row first, then the funding account row. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FixedDeposit f where f.id = :id")
    Optional<FixedDeposit> findByIdForUpdate(@Param("id") Long id);
}
