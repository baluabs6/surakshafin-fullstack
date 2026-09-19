package com.surakshafin.budget;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public class Dtos {

    public record TransactionRequest(
            @NotBlank String category,
            @NotNull @Positive BigDecimal amount,
            @NotBlank @Size(max = 120) String merchant,
            boolean isBnpl
    ) {}

    public record TransactionView(Long id, String category, BigDecimal amount, String merchant, boolean isBnpl, String occurredAt) {
        public static TransactionView from(Transaction t) {
            return new TransactionView(t.getId(), t.getCategory(), t.getAmount(), t.getMerchant(), t.isBnpl(), t.getOccurredAt().toString());
        }
    }

    public record SetBudgetRequest(@NotNull @Positive BigDecimal monthlyLimit) {}

    // New feature: per-category breakdown, computed from data that was already being tracked
    // (Transaction.category) but never surfaced beyond a single running total.
    public record CategorySpend(String category, BigDecimal amount) {}

    public record BudgetSummary(
            BigDecimal monthlyLimit,
            BigDecimal spentThisSet,
            BigDecimal remaining,
            int percentUsed,
            String nudge,
            List<CategorySpend> byCategory,
            // New feature: aggregate BNPL exposure, computed from Transaction.isBnpl which was
            // captured on every transaction but never rolled up anywhere.
            BigDecimal bnplExposure
    ) {}
}
