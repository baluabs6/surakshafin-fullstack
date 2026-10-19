package com.surakshafin.banking;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** Append-only record of every balance movement; the statement is a view over these rows. */
@Entity
@Table(name = "ledger_entries", indexes = @Index(columnList = "accountId, createdAt"))
@Getter
@Setter
@NoArgsConstructor
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntryType entryType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(nullable = false, length = 24)
    private String reference;

    @Column(length = 120)
    private String counterparty;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
