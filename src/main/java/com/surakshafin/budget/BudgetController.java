package com.surakshafin.budget;

import com.surakshafin.common.ApiResponse;
import com.surakshafin.config.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/budget")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping("/transactions")
    public ApiResponse<Dtos.TransactionView> addTransaction(@Valid @RequestBody Dtos.TransactionRequest request) {
        return ApiResponse.ok(budgetService.addTransaction(CurrentUser.id(), request), "Transaction recorded");
    }

    @GetMapping("/transactions")
    public ApiResponse<List<Dtos.TransactionView>> transactions() {
        return ApiResponse.ok(budgetService.listTransactions(CurrentUser.id()));
    }

    @PutMapping("/limit")
    public ApiResponse<Void> setBudget(@Valid @RequestBody Dtos.SetBudgetRequest request) {
        budgetService.setBudget(CurrentUser.id(), request);
        return ApiResponse.ok(null, "Budget updated");
    }

    @GetMapping("/summary")
    public ApiResponse<Dtos.BudgetSummary> summary() {
        return ApiResponse.ok(budgetService.summary(CurrentUser.id()));
    }
}
