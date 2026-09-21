package com.surakshafin.budget;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final int defaultNudgeThreshold;

    public BudgetService(TransactionRepository transactionRepository,
                          BudgetRepository budgetRepository,
                          @Value("${surakshafin.budget.default-nudge-threshold-percent}") int defaultNudgeThreshold) {
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.defaultNudgeThreshold = defaultNudgeThreshold;
    }

    public Dtos.TransactionView addTransaction(Long userId, Dtos.TransactionRequest req) {
        Transaction t = new Transaction();
        t.setUserId(userId);
        t.setCategory(req.category());
        t.setAmount(req.amount());
        t.setMerchant(req.merchant());
        t.setBnpl(req.isBnpl());
        t = transactionRepository.save(t);
        return Dtos.TransactionView.from(t);
    }

    public List<Dtos.TransactionView> listTransactions(Long userId) {
        return transactionRepository.findByUserIdOrderByOccurredAtDesc(userId)
                .stream().map(Dtos.TransactionView::from).toList();
    }

    public void setBudget(Long userId, Dtos.SetBudgetRequest req) {
        Budget budget = budgetRepository.findByUserId(userId).orElseGet(Budget::new);
        budget.setUserId(userId);
        budget.setMonthlyLimit(req.monthlyLimit());
        budgetRepository.save(budget);
    }

    public Dtos.BudgetSummary summary(Long userId) {
        List<Transaction> transactions = transactionRepository.findByUserIdOrderByOccurredAtDesc(userId);

        BigDecimal spent = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Dtos.CategorySpend> byCategory = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)))
                .entrySet().stream()
                .map(e -> new Dtos.CategorySpend(e.getKey(), e.getValue()))
                .sorted((a, b) -> b.amount().compareTo(a.amount()))
                .toList();

        BigDecimal bnplExposure = transactions.stream()
                .filter(Transaction::isBnpl)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal limit = budgetRepository.findByUserId(userId)
                .map(Budget::getMonthlyLimit)
                .orElse(BigDecimal.ZERO);

        if (limit.compareTo(BigDecimal.ZERO) == 0) {
            return new Dtos.BudgetSummary(limit, spent, BigDecimal.ZERO, 0,
                    "No monthly budget set yet — set one to get plain-language spend nudges.",
                    byCategory, bnplExposure);
        }

        BigDecimal remaining = limit.subtract(spent);
        int percentUsed = spent.multiply(BigDecimal.valueOf(100))
                .divide(limit, 0, RoundingMode.HALF_UP)
                .intValue();

        String nudge;
        if (percentUsed >= 100) {
            nudge = "You've gone past your monthly budget. Consider pausing non-essential spending.";
        } else if (percentUsed >= defaultNudgeThreshold) {
            nudge = "You've used " + percentUsed + "% of this month's budget — you're close to the limit.";
        } else {
            nudge = "You're on track — " + percentUsed + "% of this month's budget used.";
        }

        return new Dtos.BudgetSummary(limit, spent, remaining, percentUsed, nudge, byCategory, bnplExposure);
    }
}
