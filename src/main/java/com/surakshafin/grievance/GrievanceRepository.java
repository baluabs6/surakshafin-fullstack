package com.surakshafin.grievance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GrievanceRepository extends JpaRepository<Grievance, Long> {
    List<Grievance> findByUserIdOrderByCreatedAtDesc(Long userId);
}
