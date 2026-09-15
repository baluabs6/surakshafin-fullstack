package com.surakshafin.fraud;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScamPatternRepository extends JpaRepository<ScamPattern, Long> {
    List<ScamPattern> findByCategory(String category);
}
