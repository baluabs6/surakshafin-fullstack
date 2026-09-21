package com.surakshafin.fraud;

import com.surakshafin.common.ApiResponse;
import com.surakshafin.config.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fraud")
public class FraudController {

    private final FraudService fraudService;

    public FraudController(FraudService fraudService) {
        this.fraudService = fraudService;
    }

    @GetMapping("/patterns")
    public ApiResponse<List<Dtos.ScamPatternView>> patterns(@RequestParam(required = false) String category) {
        return ApiResponse.ok(fraudService.listPatterns(category));
    }

    @PostMapping("/reports")
    public ApiResponse<Dtos.ReportView> report(@Valid @RequestBody Dtos.ReportRequest request) {
        return ApiResponse.ok(fraudService.submitReport(CurrentUser.id(), request), "Report submitted");
    }

    @GetMapping("/reports/mine")
    public ApiResponse<List<Dtos.ReportView>> myReports() {
        return ApiResponse.ok(fraudService.myReports(CurrentUser.id()));
    }

    @PatchMapping("/reports/{id}/status")
    public ApiResponse<Dtos.ReportView> updateStatus(@PathVariable Long id, @Valid @RequestBody Dtos.UpdateStatusRequest request) {
        return ApiResponse.ok(fraudService.updateStatus(id, request), "Status updated");
    }

    @PostMapping("/patterns/{id}/confirm")
    public ApiResponse<Dtos.ScamPatternView> confirmPattern(@PathVariable Long id) {
        return ApiResponse.ok(fraudService.confirmPattern(id), "Thanks for confirming");
    }

    @PostMapping("/pre-transaction-check")
    public ApiResponse<Dtos.PreTransactionCheckResponse> preTransactionCheck(@Valid @RequestBody Dtos.PreTransactionCheckRequest request) {
        return ApiResponse.ok(fraudService.checkBeforeTransaction(CurrentUser.id(), request));
    }

    @PostMapping("/trusted-payees")
    public ApiResponse<Dtos.TrustedPayeeView> addTrustedPayee(@Valid @RequestBody Dtos.TrustedPayeeRequest request) {
        return ApiResponse.ok(fraudService.addTrustedPayee(CurrentUser.id(), request), "Payee trusted");
    }

    @GetMapping("/trusted-payees")
    public ApiResponse<List<Dtos.TrustedPayeeView>> trustedPayees() {
        return ApiResponse.ok(fraudService.listTrustedPayees(CurrentUser.id()));
    }

    @DeleteMapping("/trusted-payees/{id}")
    public ApiResponse<Void> removeTrustedPayee(@PathVariable Long id) {
        fraudService.removeTrustedPayee(CurrentUser.id(), id);
        return ApiResponse.ok(null, "Payee removed");
    }
}
