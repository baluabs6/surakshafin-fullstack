package com.surakshafin.banking;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "fixed_deposits", indexes = @Index(columnList = "userId"))
@Getter
@Setter
@NoArgsConstructor
public class FixedDeposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /** Savings/current account that funded the FD and receives the proceeds. */
    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal principal;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal ratePercent;

    @Column(nullable = false)
    private int tenureMonths;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal maturityAmount;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate maturityDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FdStatus status = FdStatus.ACTIVE;

    /** Amount actually paid out on close/maturity. */
    @Column(precision = 19, scale = 2)
    private BigDecimal payoutAmount;

    private Instant closedAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
