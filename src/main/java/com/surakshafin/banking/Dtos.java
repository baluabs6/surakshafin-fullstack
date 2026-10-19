package com.surakshafin.banking;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class Dtos {

    private static final String IFSC_REGEX = "^[A-Za-z]{4}0[A-Za-z0-9]{6}$";

    // ---- accounts ----------------------------------------------------------------------

    public record OpenAccountRequest(@NotNull AccountType type) {}

    public record AccountView(Long id, String accountNumber, String ifsc, AccountType type, AccountStatus status,
                              BigDecimal balance, BigDecimal dailyTransferLimit, String createdAt) {
        public static AccountView from(BankAccount a) {
            return new AccountView(a.getId(), a.getAccountNumber(), a.getIfsc(), a.getAccountType(), a.getStatus(),
                    a.getBalance(), a.getDailyTransferLimit(), a.getCreatedAt().toString());
        }
    }

    public record AmountRequest(
            @NotNull @DecimalMin("1.00") @Digits(integer = 12, fraction = 2) BigDecimal amount,
            @Size(max = 120) String remark
    ) {}

    public record UpdateAccountStatusRequest(@NotNull AccountStatus status) {}

    public record LedgerEntryView(Long id, EntryType type, Channel channel, BigDecimal amount, BigDecimal balanceAfter,
                                  String description, String reference, String counterparty, String createdAt) {
        public static LedgerEntryView from(LedgerEntry e) {
            return new LedgerEntryView(e.getId(), e.getEntryType(), e.getChannel(), e.getAmount(), e.getBalanceAfter(),
                    e.getDescription(), e.getReference(), e.getCounterparty(), e.getCreatedAt().toString());
        }
    }

    public record StatementView(Long accountId, String accountNumber, LocalDate from, LocalDate to,
                                List<LedgerEntryView> entries, int page, int size, long totalElements, int totalPages) {}

    // ---- beneficiaries -----------------------------------------------------------------

    public record BeneficiaryRequest(
            @NotBlank @Pattern(regexp = "^\\d{9,16}$", message = "must be 9 to 16 digits") String accountNumber,
            @NotBlank @Pattern(regexp = IFSC_REGEX, message = "is not a valid IFSC") String ifsc,
            @Size(max = 60) String nickname,
            boolean markTrusted
    ) {}

    public record BeneficiaryView(Long id, String accountNumber, String ifsc, String maskedName, String nickname, String createdAt) {
        public static BeneficiaryView from(Beneficiary b) {
            return new BeneficiaryView(b.getId(), b.getAccountNumber(), b.getIfsc(), b.getMaskedName(), b.getNickname(),
                    b.getCreatedAt().toString());
        }
    }

    // ---- transfers ---------------------------------------------------------------------

    public record TransferCheckRequest(
            @NotNull Long fromAccountId,
            @NotBlank @Pattern(regexp = "^\\d{9,16}$", message = "must be 9 to 16 digits") String toAccountNumber,
            @NotBlank @Pattern(regexp = IFSC_REGEX, message = "is not a valid IFSC") String toIfsc,
            @NotNull @DecimalMin("1.00") @Digits(integer = 12, fraction = 2) BigDecimal amount
    ) {}

    public record TransferCheckResponse(String payeeMaskedName, String riskLevel, List<String> reasons,
                                        List<String> checklistPrompts, boolean requiresAcknowledgement, boolean blocked) {}

    public record TransferRequest(
            @NotNull Long fromAccountId,
            @NotBlank @Pattern(regexp = "^\\d{9,16}$", message = "must be 9 to 16 digits") String toAccountNumber,
            @NotBlank @Pattern(regexp = IFSC_REGEX, message = "is not a valid IFSC") String toIfsc,
            @NotNull @DecimalMin("1.00") @Digits(integer = 12, fraction = 2) BigDecimal amount,
            @NotNull TransferMode mode,
            @Size(max = 120) String remark,
            /** Client-generated unique value (e.g. a UUID). Retrying with the same key never moves money twice. */
            @NotBlank @Size(max = 64) String idempotencyKey,
            /** Must be true to proceed when the pre-transfer fraud check returns MEDIUM risk. */
            boolean acknowledgeRisk
    ) {}

    public record TransferView(String reference, Long fromAccountId, String toAccountNumber, String toMaskedName,
                               BigDecimal amount, TransferMode mode, String remark, String status, String createdAt) {}

    // ---- fixed deposits ----------------------------------------------------------------

    public record FdQuoteResponse(BigDecimal principal, int tenureMonths, BigDecimal ratePercent,
                                  BigDecimal maturityAmount, BigDecimal interestEarned, LocalDate maturityDate) {}

    public record BookFdRequest(
            @NotNull Long accountId,
            @NotNull @DecimalMin("1000.00") @Digits(integer = 12, fraction = 2) BigDecimal principal,
            @Min(3) @Max(120) int tenureMonths
    ) {}

    public record FdView(Long id, Long accountId, BigDecimal principal, BigDecimal ratePercent, int tenureMonths,
                         BigDecimal maturityAmount, LocalDate startDate, LocalDate maturityDate, FdStatus status,
                         BigDecimal payoutAmount, String closedAt) {
        public static FdView from(FixedDeposit f) {
            return new FdView(f.getId(), f.getAccountId(), f.getPrincipal(), f.getRatePercent(), f.getTenureMonths(),
                    f.getMaturityAmount(), f.getStartDate(), f.getMaturityDate(), f.getStatus(), f.getPayoutAmount(),
                    f.getClosedAt() == null ? null : f.getClosedAt().toString());
        }
    }

    // ---- loans / EMI -------------------------------------------------------------------

    public record EmiInstallment(int number, BigDecimal emi, BigDecimal principalPart, BigDecimal interestPart, BigDecimal balance) {}

    public record EmiResponse(BigDecimal principal, BigDecimal annualRatePercent, int tenureMonths, BigDecimal emi,
                              BigDecimal totalInterest, BigDecimal totalPayable, List<EmiInstallment> schedule) {}
}
