package com.surakshafin.banking;

import com.surakshafin.common.BadRequestException;
import com.surakshafin.common.NotFoundException;
import com.surakshafin.identity.IdentityService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Owns accounts and the ledger. Every balance change in the banking module goes through
 * {@link #credit} / {@link #debit}, which require the caller to already hold the account row lock.
 */
@Service
public class AccountService {

    private static final SecureRandom RANDOM = new SecureRandom();

    @PersistenceContext
    private EntityManager entityManager;

    private final BankAccountRepository accountRepository;
    private final LedgerEntryRepository ledgerRepository;
    private final IdentityService identityService;
    private final String ifsc;
    private final ZoneId zone;
    private final BigDecimal defaultDailyLimit;
    private final int maxAccountsPerUser;
    private final boolean demoDepositsEnabled;
    private final BigDecimal maxDeposit;

    public AccountService(BankAccountRepository accountRepository,
                          LedgerEntryRepository ledgerRepository,
                          IdentityService identityService,
                          @Value("${surakshafin.banking.ifsc}") String ifsc,
                          @Value("${surakshafin.banking.timezone:Asia/Kolkata}") String timezone,
                          @Value("${surakshafin.banking.default-daily-transfer-limit}") BigDecimal defaultDailyLimit,
                          @Value("${surakshafin.banking.max-accounts-per-user}") int maxAccountsPerUser,
                          @Value("${surakshafin.banking.demo-deposits-enabled}") boolean demoDepositsEnabled,
                          @Value("${surakshafin.banking.max-deposit-amount}") BigDecimal maxDeposit) {
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
        this.identityService = identityService;
        this.ifsc = ifsc;
        this.zone = ZoneId.of(timezone);
        this.defaultDailyLimit = defaultDailyLimit;
        this.maxAccountsPerUser = maxAccountsPerUser;
        this.demoDepositsEnabled = demoDepositsEnabled;
        this.maxDeposit = maxDeposit;
    }

    // ---- account lifecycle ---------------------------------------------------------------

    @Transactional
    public Dtos.AccountView open(Long userId, Dtos.OpenAccountRequest req) {
        if (!identityService.getProfile(userId).kycLiteVerified()) {
            throw new BadRequestException("Complete KYC verification before opening a bank account");
        }
        if (accountRepository.countByUserId(userId) >= maxAccountsPerUser) {
            throw new BadRequestException("You can hold at most " + maxAccountsPerUser + " accounts");
        }
        BankAccount a = new BankAccount();
        a.setUserId(userId);
        a.setAccountType(req.type());
        a.setIfsc(ifsc);
        a.setAccountNumber(newAccountNumber());
        a.setDailyTransferLimit(defaultDailyLimit);
        return Dtos.AccountView.from(accountRepository.save(a));
    }

    @Transactional(readOnly = true)
    public List<Dtos.AccountView> list(Long userId) {
        return accountRepository.findByUserIdOrderByCreatedAtAscIdAsc(userId).stream().map(Dtos.AccountView::from).toList();
    }

    @Transactional(readOnly = true)
    public Dtos.AccountView get(Long userId, Long accountId) {
        return Dtos.AccountView.from(ownedAccount(userId, accountId));
    }

    /** Operator action: freeze or unfreeze an account. */
    @Transactional
    public Dtos.AccountView updateStatus(Long accountId, AccountStatus status) {
        BankAccount a = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found"));
        a.setStatus(status);
        return Dtos.AccountView.from(accountRepository.save(a));
    }

    // ---- cash movements ------------------------------------------------------------------

    /** Demo funding source. There is no real money rail here, so this is switchable off in config. */
    @Transactional
    public Dtos.AccountView deposit(Long userId, Long accountId, Dtos.AmountRequest req) {
        if (!demoDepositsEnabled) {
            throw new BadRequestException("Deposits are disabled in this environment");
        }
        BigDecimal amount = money(req.amount());
        if (amount.compareTo(maxDeposit) > 0) {
            throw new BadRequestException("A single deposit cannot exceed " + maxDeposit.toPlainString());
        }
        BankAccount a = lockOwned(userId, accountId);
        credit(a, Channel.DEPOSIT, amount, describe("Cash deposit", req.remark()), newReference(), null);
        return Dtos.AccountView.from(a);
    }

    @Transactional
    public Dtos.AccountView withdraw(Long userId, Long accountId, Dtos.AmountRequest req) {
        BankAccount a = lockOwned(userId, accountId);
        debit(a, Channel.WITHDRAWAL, money(req.amount()), describe("Cash withdrawal", req.remark()), newReference(), null);
        return Dtos.AccountView.from(a);
    }

    // ---- statements ----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Dtos.StatementView statement(Long userId, Long accountId, LocalDate from, LocalDate to, int page, int size) {
        BankAccount a = ownedAccount(userId, accountId);
        LocalDate end = to == null ? LocalDate.now(zone) : to;
        LocalDate start = from == null ? end.minusDays(30) : from;
        validateRange(start, end);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<LedgerEntry> result = ledgerRepository.findStatement(a.getId(), startOf(start), startOf(end.plusDays(1)),
                PageRequest.of(Math.max(page, 0), safeSize));
        return new Dtos.StatementView(a.getId(), a.getAccountNumber(), start, end,
                result.getContent().stream().map(Dtos.LedgerEntryView::from).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public String statementCsv(Long userId, Long accountId, LocalDate from, LocalDate to) {
        BankAccount a = ownedAccount(userId, accountId);
        LocalDate end = to == null ? LocalDate.now(zone) : to;
        LocalDate start = from == null ? end.minusDays(30) : from;
        validateRange(start, end);
        List<LedgerEntry> entries = ledgerRepository.findStatementAscending(a.getId(), startOf(start),
                startOf(end.plusDays(1)), PageRequest.of(0, 5000));
        StringBuilder sb = new StringBuilder("Date,Reference,Description,Counterparty,Type,Debit,Credit,Balance\n");
        for (LedgerEntry e : entries) {
            boolean debit = e.getEntryType() == EntryType.DEBIT;
            sb.append(csv(e.getCreatedAt().atZone(zone).toLocalDateTime().toString())).append(',')
                    .append(csv(e.getReference())).append(',')
                    .append(csv(e.getDescription())).append(',')
                    .append(csv(e.getCounterparty())).append(',')
                    .append(csv(e.getChannel().name())).append(',')
                    .append(debit ? e.getAmount().toPlainString() : "").append(',')
                    .append(debit ? "" : e.getAmount().toPlainString()).append(',')
                    .append(e.getBalanceAfter().toPlainString()).append('\n');
        }
        return sb.toString();
    }

    // ---- shared helpers for the other banking services (package-private on purpose) -----------

    BankAccount ownedAccount(Long userId, Long accountId) {
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new NotFoundException("Account not found"));
    }

    /** Locks the row, then verifies ownership, so a foreign id leaks nothing about the account. */
    BankAccount lockOwned(Long userId, Long accountId) {
        BankAccount a = lockById(accountId);
        if (!a.getUserId().equals(userId)) {
            throw new NotFoundException("Account not found");
        }
        return a;
    }

    /**
     * Takes the row lock and then re-reads the row. The refresh matters: if this transaction already
     * loaded the account (e.g. to validate ownership) the lock query alone would hand back that stale
     * in-memory copy, and a balance changed by another transaction in between would be overwritten.
     */
    BankAccount lockById(Long accountId) {
        BankAccount a = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found"));
        entityManager.refresh(a);
        return a;
    }

    BigDecimal transfersSentToday(Long accountId) {
        return ledgerRepository.sumSince(accountId, Channel.TRANSFER_OUT, LocalDate.now(zone).atStartOfDay(zone).toInstant());
    }

    LocalDate today() {
        return LocalDate.now(zone);
    }

    String holderMaskedName(BankAccount a) {
        return maskName(identityService.getProfile(a.getUserId()).fullName());
    }

    LedgerEntry credit(BankAccount a, Channel channel, BigDecimal amount, String description, String reference, String counterparty) {
        a.setBalance(a.getBalance().add(amount));
        accountRepository.save(a);
        return record(a, EntryType.CREDIT, channel, amount, description, reference, counterparty);
    }

    LedgerEntry debit(BankAccount a, Channel channel, BigDecimal amount, String description, String reference, String counterparty) {
        if (a.getStatus() != AccountStatus.ACTIVE) {
            throw new BadRequestException("This account is frozen. Please contact support.");
        }
        if (a.getBalance().compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient balance");
        }
        a.setBalance(a.getBalance().subtract(amount));
        accountRepository.save(a);
        return record(a, EntryType.DEBIT, channel, amount, description, reference, counterparty);
    }

    static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.UNNECESSARY);
    }

    static String newReference() {
        return "SF" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase();
    }

    static String describe(String base, String remark) {
        return remark == null || remark.isBlank() ? base : base + " - " + remark.trim();
    }

    /** "Balasubrahmanyam A" -> "B***** A". Enough for the payer to recognise the payee, not to harvest names. */
    static String maskName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "****";
        }
        StringBuilder out = new StringBuilder();
        for (String part : fullName.trim().split("\\s+")) {
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(part.charAt(0)).append("*".repeat(Math.min(part.length() - 1, 5)));
        }
        return out.toString();
    }

    // ---- internals -----------------------------------------------------------------------

    private LedgerEntry record(BankAccount a, EntryType type, Channel channel, BigDecimal amount,
                               String description, String reference, String counterparty) {
        LedgerEntry e = new LedgerEntry();
        e.setAccountId(a.getId());
        e.setEntryType(type);
        e.setChannel(channel);
        e.setAmount(amount);
        e.setBalanceAfter(a.getBalance());
        e.setDescription(description.length() > 200 ? description.substring(0, 200) : description);
        e.setReference(reference);
        e.setCounterparty(counterparty);
        return ledgerRepository.save(e);
    }

    private String newAccountNumber() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder().append(1 + RANDOM.nextInt(9));
            for (int i = 0; i < 11; i++) {
                sb.append(RANDOM.nextInt(10));
            }
            String candidate = sb.toString();
            if (!accountRepository.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not allocate an account number");
    }

    private Instant startOf(LocalDate d) {
        return d.atStartOfDay(zone).toInstant();
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BadRequestException("'from' must not be after 'to'");
        }
        if (from.plusDays(366).isBefore(to)) {
            throw new BadRequestException("A statement can cover at most one year");
        }
    }

    /** Quotes the field and neutralises spreadsheet formula injection from user-supplied remarks. */
    private static String csv(String v) {
        if (v == null) {
            return "";
        }
        String s = v;
        if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) {
            s = "'" + s;
        }
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
