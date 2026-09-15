package com.surakshafin.budget;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "spend_transactions")
@Getter
@Setter
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String category; // GROCERIES, BNPL_EMI, SUBSCRIPTIONS, DINING, TRAVEL, OTHER

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String merchant;

    private boolean isBnpl = false;

    @Column(nullable = false)
    private Instant occurredAt = Instant.now();
}
