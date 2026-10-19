package com.surakshafin.banking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    @Query("""
            select e from LedgerEntry e
            where e.accountId = :accountId and e.createdAt >= :from and e.createdAt < :to
            order by e.createdAt desc, e.id desc
            """)
    Page<LedgerEntry> findStatement(@Param("accountId") Long accountId,
                                    @Param("from") Instant from,
                                    @Param("to") Instant to,
                                    Pageable pageable);

    @Query("""
            select e from LedgerEntry e
            where e.accountId = :accountId and e.createdAt >= :from and e.createdAt < :to
            order by e.createdAt asc, e.id asc
            """)
    List<LedgerEntry> findStatementAscending(@Param("accountId") Long accountId,
                                             @Param("from") Instant from,
                                             @Param("to") Instant to,
                                             Pageable pageable);

    @Query("""
            select coalesce(sum(e.amount), 0) from LedgerEntry e
            where e.accountId = :accountId and e.channel = :channel and e.createdAt >= :since
            """)
    BigDecimal sumSince(@Param("accountId") Long accountId,
                        @Param("channel") Channel channel,
                        @Param("since") Instant since);
}
