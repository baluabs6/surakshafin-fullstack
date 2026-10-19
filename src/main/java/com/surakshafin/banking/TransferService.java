package com.surakshafin.banking;

import com.surakshafin.common.BadRequestException;
import com.surakshafin.common.ConflictException;
import com.surakshafin.common.NotFoundException;
import com.surakshafin.fraud.FraudService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Account-to-account transfers between SuRakshaFin accounts, plus beneficiary management.
 * Every transfer is screened by the fraud module before any money moves.
 */
@Service
public class TransferService {

    private final BankAccountRepository accountRepository;
    private final BankTransferRepository transferRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final AccountService accountService;
    private final FraudService fraudService;

    public TransferService(BankAccountRepository accountRepository,
                           BankTransferRepository transferRepository,
                           BeneficiaryRepository beneficiaryRepository,
                           AccountService accountService,
                           FraudService fraudService) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.accountService = accountService;
        this.fraudService = fraudService;
    }

    // ---- beneficiaries -------------------------------------------------------------------

    @Transactional
    public Dtos.BeneficiaryView addBeneficiary(Long userId, Dtos.BeneficiaryRequest req) {
        BankAccount target = resolve(req.accountNumber(), req.ifsc());
        if (target.getUserId().equals(userId)) {
            throw new BadRequestException("That is one of your own accounts. You can transfer to it directly.");
        }
        if (beneficiaryRepository.findByUserIdAndAccountNumber(userId, target.getAccountNumber()).isPresent()) {
            throw new BadRequestException("This beneficiary is already saved");
        }
        Beneficiary b = new Beneficiary();
        b.setUserId(userId);
        b.setAccountNumber(target.getAccountNumber());
        b.setIfsc(target.getIfsc());
        b.setMaskedName(accountService.holderMaskedName(target));
        b.setNickname(req.nickname() == null || req.nickname().isBlank() ? null : req.nickname().trim());
        b = beneficiaryRepository.save(b);

        if (req.markTrusted()) {
            try {
                fraudService.addTrustedPayee(userId,
                        new com.surakshafin.fraud.Dtos.TrustedPayeeRequest(payeeIdentifier(target), b.getNickname()));
            } catch (BadRequestException alreadyTrusted) {
                // Already on the trusted list: nothing to do.
            }
        }
        return Dtos.BeneficiaryView.from(b);
    }

    @Transactional(readOnly = true)
    public List<Dtos.BeneficiaryView> listBeneficiaries(Long userId) {
        return beneficiaryRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(Dtos.BeneficiaryView::from).toList();
    }

    @Transactional
    public void removeBeneficiary(Long userId, Long id) {
        Beneficiary b = beneficiaryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Beneficiary not found"));
        beneficiaryRepository.delete(b);
    }

    // ---- transfers -----------------------------------------------------------------------

    /** Dry run shown to the user before they confirm: who the payee is and how risky the fraud module thinks it is. */
    @Transactional(readOnly = true)
    public Dtos.TransferCheckResponse check(Long userId, Dtos.TransferCheckRequest req) {
        BankAccount source = accountService.ownedAccount(userId, req.fromAccountId());
        BankAccount dest = resolve(req.toAccountNumber(), req.toIfsc());
        requireAllowedDestination(userId, source, dest);
        var risk = screen(userId, dest, AccountService.money(req.amount()));
        boolean blocked = "HIGH".equals(risk.riskLevel());
        boolean needsAck = "MEDIUM".equals(risk.riskLevel());
        return new Dtos.TransferCheckResponse(accountService.holderMaskedName(dest), risk.riskLevel(),
                risk.reasons(), risk.checklistPrompts(), needsAck, blocked);
    }

    @Transactional
    public Dtos.TransferView transfer(Long userId, Dtos.TransferRequest req) {
        BigDecimal amount = AccountService.money(req.amount());
        TransferMode mode = req.mode();
        if (amount.compareTo(mode.min()) < 0) {
            throw new BadRequestException(mode + " transfers must be at least " + mode.min().toPlainString());
        }
        if (mode.max() != null && amount.compareTo(mode.max()) > 0) {
            throw new BadRequestException(mode + " transfers cannot exceed " + mode.max().toPlainString());
        }

        BankAccount sourceView = accountService.ownedAccount(userId, req.fromAccountId());
        BankAccount destView = resolve(req.toAccountNumber(), req.toIfsc());
        if (sourceView.getId().equals(destView.getId())) {
            throw new BadRequestException("Source and destination accounts must be different");
        }
        requireAllowedDestination(userId, sourceView, destView);

        // Fraud screening happens before locks are taken, and before any money moves.
        var risk = screen(userId, destView, amount);
        if ("HIGH".equals(risk.riskLevel())) {
            throw new BadRequestException("Transfer blocked: " + String.join("; ", risk.reasons()));
        }
        if ("MEDIUM".equals(risk.riskLevel()) && !req.acknowledgeRisk()) {
            throw new ConflictException("Please review the safety warnings and confirm to continue: "
                    + String.join("; ", risk.reasons()));
        }

        // Lock both rows in ascending id order so two opposite transfers can never deadlock.
        Long firstId = Math.min(sourceView.getId(), destView.getId());
        Long secondId = Math.max(sourceView.getId(), destView.getId());
        BankAccount first = accountService.lockById(firstId);
        BankAccount second = accountService.lockById(secondId);
        BankAccount source = first.getId().equals(sourceView.getId()) ? first : second;
        BankAccount dest = source == first ? second : first;

        // Idempotency is checked under the source lock, so concurrent retries serialise here.
        var existing = transferRepository.findByUserIdAndIdempotencyKey(userId, req.idempotencyKey());
        if (existing.isPresent()) {
            BankTransfer t = existing.get();
            boolean same = t.getFromAccountId().equals(source.getId()) && t.getToAccountId().equals(dest.getId())
                    && t.getAmount().compareTo(amount) == 0 && t.getMode() == mode;
            if (!same) {
                throw new ConflictException("This idempotency key was already used for a different transfer");
            }
            return toView(t, dest);
        }

        BigDecimal movedToday = accountService.transfersSentToday(source.getId());
        if (movedToday.add(amount).compareTo(source.getDailyTransferLimit()) > 0) {
            BigDecimal left = source.getDailyTransferLimit().subtract(movedToday).max(BigDecimal.ZERO);
            throw new BadRequestException("Daily transfer limit exceeded. You can still send up to " + left.toPlainString() + " today.");
        }

        String reference = AccountService.newReference();
        String destMasked = accountService.holderMaskedName(dest);
        String sourceMasked = accountService.holderMaskedName(source);
        accountService.debit(source, Channel.TRANSFER_OUT, amount,
                AccountService.describe(mode + " transfer to " + destMasked, req.remark()), reference,
                lastFour(dest.getAccountNumber()) + " " + destMasked);
        accountService.credit(dest, Channel.TRANSFER_IN, amount,
                AccountService.describe(mode + " transfer from " + sourceMasked, req.remark()), reference,
                lastFour(source.getAccountNumber()) + " " + sourceMasked);

        BankTransfer t = new BankTransfer();
        t.setReference(reference);
        t.setUserId(userId);
        t.setFromAccountId(source.getId());
        t.setToAccountId(dest.getId());
        t.setAmount(amount);
        t.setMode(mode);
        t.setRemark(req.remark() == null || req.remark().isBlank() ? null : req.remark().trim());
        t.setIdempotencyKey(req.idempotencyKey());
        return toView(transferRepository.save(t), dest);
    }

    @Transactional(readOnly = true)
    public List<Dtos.TransferView> recent(Long userId) {
        List<BankTransfer> transfers = transferRepository.findTop50ByUserIdOrderByCreatedAtDescIdDesc(userId);
        Map<Long, BankAccount> dests = accountRepository
                .findAllById(transfers.stream().map(BankTransfer::getToAccountId).distinct().toList())
                .stream().collect(Collectors.toMap(BankAccount::getId, Function.identity()));
        return transfers.stream().map(t -> toView(t, dests.get(t.getToAccountId()))).toList();
    }

    // ---- internals -----------------------------------------------------------------------

    private com.surakshafin.fraud.Dtos.PreTransactionCheckResponse screen(Long userId, BankAccount dest, BigDecimal amount) {
        if (dest.getUserId().equals(userId)) {
            return new com.surakshafin.fraud.Dtos.PreTransactionCheckResponse("LOW", List.of(), List.of(), false, 0);
        }
        return fraudService.checkBeforeTransaction(userId,
                new com.surakshafin.fraud.Dtos.PreTransactionCheckRequest(payeeIdentifier(dest), null, amount));
    }

    /** Same shape as a UPI VPA for account+IFSC payees, so fraud reports and trusted payees can reference it. */
    static String payeeIdentifier(BankAccount dest) {
        return dest.getAccountNumber() + "@" + dest.getIfsc().toUpperCase() + ".ifsc.npci";
    }

    private BankAccount resolve(String accountNumber, String ifsc) {
        // One generic message for both "no such number" and "IFSC mismatch" so the API is not an oracle.
        return accountRepository.findByAccountNumber(accountNumber)
                .filter(a -> a.getIfsc().equalsIgnoreCase(ifsc))
                .orElseThrow(() -> new NotFoundException("No account found for these details"));
    }

    private void requireAllowedDestination(Long userId, BankAccount source, BankAccount dest) {
        if (source.getId().equals(dest.getId())) {
            throw new BadRequestException("Source and destination accounts must be different");
        }
        boolean own = dest.getUserId().equals(userId);
        if (!own && beneficiaryRepository.findByUserIdAndAccountNumber(userId, dest.getAccountNumber()).isEmpty()) {
            throw new BadRequestException("Add this account as a beneficiary before sending money to it");
        }
    }

    private Dtos.TransferView toView(BankTransfer t, BankAccount dest) {
        return new Dtos.TransferView(t.getReference(), t.getFromAccountId(), dest.getAccountNumber(),
                accountService.holderMaskedName(dest), t.getAmount(), t.getMode(), t.getRemark(), "COMPLETED",
                t.getCreatedAt().toString());
    }

    private static String lastFour(String accountNumber) {
        return "XXXX" + accountNumber.substring(Math.max(accountNumber.length() - 4, 0));
    }
}
