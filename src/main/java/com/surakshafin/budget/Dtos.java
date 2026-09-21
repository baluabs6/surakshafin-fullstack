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

    public record CategorySpend(String category, BigDecimal amount) {}

    public record BudgetSummary(
            BigDecimal monthlyLimit,
            BigDecimal spentThisSet,
            BigDecimal remaining,
            int percentUsed,
            String nudge,
            List<CategorySpend> byCategory,
            BigDecimal bnplExposure
    ) {}
}
