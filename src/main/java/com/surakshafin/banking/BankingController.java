package com.surakshafin.banking;

import com.surakshafin.common.ApiResponse;
import com.surakshafin.config.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/banking")
public class BankingController {

    private final AccountService accountService;
    private final TransferService transferService;
    private final FixedDepositService fixedDepositService;

    public BankingController(AccountService accountService, TransferService transferService,
                             FixedDepositService fixedDepositService) {
        this.accountService = accountService;
        this.transferService = transferService;
        this.fixedDepositService = fixedDepositService;
    }

    // ---- accounts ------------------------------------------------------------------------

    @PostMapping("/accounts")
    public ApiResponse<Dtos.AccountView> open(@Valid @RequestBody Dtos.OpenAccountRequest req) {
        return ApiResponse.ok(accountService.open(CurrentUser.id(), req), "Account opened");
    }

    @GetMapping("/accounts")
    public ApiResponse<List<Dtos.AccountView>> accounts() {
        return ApiResponse.ok(accountService.list(CurrentUser.id()));
    }

    @GetMapping("/accounts/{id}")
    public ApiResponse<Dtos.AccountView> account(@PathVariable Long id) {
        return ApiResponse.ok(accountService.get(CurrentUser.id(), id));
    }

    @PostMapping("/accounts/{id}/deposit")
    public ApiResponse<Dtos.AccountView> deposit(@PathVariable Long id, @Valid @RequestBody Dtos.AmountRequest req) {
        return ApiResponse.ok(accountService.deposit(CurrentUser.id(), id, req), "Deposit successful");
    }

    @PostMapping("/accounts/{id}/withdraw")
    public ApiResponse<Dtos.AccountView> withdraw(@PathVariable Long id, @Valid @RequestBody Dtos.AmountRequest req) {
        return ApiResponse.ok(accountService.withdraw(CurrentUser.id(), id, req), "Withdrawal successful");
    }

    /** Admin only (see SecurityConfig). */
    @PatchMapping("/accounts/{id}/status")
    public ApiResponse<Dtos.AccountView> updateStatus(@PathVariable Long id, @Valid @RequestBody Dtos.UpdateAccountStatusRequest req) {
        return ApiResponse.ok(accountService.updateStatus(id, req.status()), "Account status updated");
    }

    @GetMapping("/accounts/{id}/statement")
    public ApiResponse<Dtos.StatementView> statement(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(accountService.statement(CurrentUser.id(), id, from, to, page, size));
    }

    @GetMapping("/accounts/{id}/statement.csv")
    public ResponseEntity<byte[]> statementCsv(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        String csv = accountService.statementCsv(CurrentUser.id(), id, from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"statement-" + id + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    // ---- beneficiaries & transfers ---------------------------------------------------------

    @GetMapping("/beneficiaries")
    public ApiResponse<List<Dtos.BeneficiaryView>> beneficiaries() {
        return ApiResponse.ok(transferService.listBeneficiaries(CurrentUser.id()));
    }

    @PostMapping("/beneficiaries")
    public ApiResponse<Dtos.BeneficiaryView> addBeneficiary(@Valid @RequestBody Dtos.BeneficiaryRequest req) {
        return ApiResponse.ok(transferService.addBeneficiary(CurrentUser.id(), req), "Beneficiary added");
    }

    @DeleteMapping("/beneficiaries/{id}")
    public ApiResponse<Void> removeBeneficiary(@PathVariable Long id) {
        transferService.removeBeneficiary(CurrentUser.id(), id);
        return ApiResponse.ok(null, "Beneficiary removed");
    }

    @PostMapping("/transfers/check")
    public ApiResponse<Dtos.TransferCheckResponse> checkTransfer(@Valid @RequestBody Dtos.TransferCheckRequest req) {
        return ApiResponse.ok(transferService.check(CurrentUser.id(), req));
    }

    @PostMapping("/transfers")
    public ApiResponse<Dtos.TransferView> transfer(@Valid @RequestBody Dtos.TransferRequest req) {
        return ApiResponse.ok(transferService.transfer(CurrentUser.id(), req), "Transfer completed");
    }

    @GetMapping("/transfers")
    public ApiResponse<List<Dtos.TransferView>> transfers() {
        return ApiResponse.ok(transferService.recent(CurrentUser.id()));
    }

    // ---- fixed deposits ---------------------------------------------------------------------

    @GetMapping("/fixed-deposits/quote")
    public ApiResponse<Dtos.FdQuoteResponse> quote(@RequestParam BigDecimal principal, @RequestParam int tenureMonths) {
        return ApiResponse.ok(fixedDepositService.quote(principal, tenureMonths));
    }

    @PostMapping("/fixed-deposits")
    public ApiResponse<Dtos.FdView> bookFd(@Valid @RequestBody Dtos.BookFdRequest req) {
        return ApiResponse.ok(fixedDepositService.book(CurrentUser.id(), req), "Fixed deposit booked");
    }

    @GetMapping("/fixed-deposits")
    public ApiResponse<List<Dtos.FdView>> fixedDeposits() {
        return ApiResponse.ok(fixedDepositService.list(CurrentUser.id()));
    }

    @PostMapping("/fixed-deposits/{id}/close")
    public ApiResponse<Dtos.FdView> closeFd(@PathVariable Long id) {
        return ApiResponse.ok(fixedDepositService.close(CurrentUser.id(), id), "Fixed deposit closed");
    }

    // ---- loans -------------------------------------------------------------------------------

    @GetMapping("/loans/emi")
    public ApiResponse<Dtos.EmiResponse> emi(@RequestParam BigDecimal principal,
                                             @RequestParam BigDecimal annualRatePercent,
                                             @RequestParam int tenureMonths) {
        return ApiResponse.ok(EmiCalculator.calculate(principal, annualRatePercent, tenureMonths));
    }
}
