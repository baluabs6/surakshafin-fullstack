package com.surakshafin.banking;

import com.surakshafin.common.BadRequestException;
import com.surakshafin.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Fixed deposits: quarterly-compounded interest, a tenure-based rate card, and premature closure
 * with a 1 percentage-point penalty (no interest if closed within 7 days).
 */
@Service
public class FixedDepositService {

    private static final BigDecimal PENALTY_POINTS = BigDecimal.ONE;
    private static final int MIN_DAYS_FOR_INTEREST = 7;

    private final FixedDepositRepository fdRepository;
    private final AccountService accountService;

    public FixedDepositService(FixedDepositRepository fdRepository, AccountService accountService) {
        this.fdRepository = fdRepository;
        this.accountService = accountService;
    }

    /** Illustrative rate card. A real bank would load this from a product table. */
    static BigDecimal rateFor(int months) {
        if (months < 6) return new BigDecimal("6.00");
        if (months < 12) return new BigDecimal("6.50");
        if (months < 24) return new BigDecimal("7.00");
        if (months < 60) return new BigDecimal("7.25");
        return new BigDecimal("7.00");
    }

    static BigDecimal maturityAmount(BigDecimal principal, BigDecimal ratePercent, int months) {
        double quarterlyRate = ratePercent.doubleValue() / 400.0;
        double factor = Math.pow(1.0 + quarterlyRate, months / 3.0);
        return principal.multiply(BigDecimal.valueOf(factor)).setScale(2, RoundingMode.HALF_UP);
    }

    public Dtos.FdQuoteResponse quote(BigDecimal principal, int months) {
        validate(principal, months);
        BigDecimal p = AccountService.money(principal);
        BigDecimal rate = rateFor(months);
        BigDecimal maturity = maturityAmount(p, rate, months);
        return new Dtos.FdQuoteResponse(p, months, rate, maturity, maturity.subtract(p),
                accountService.today().plusMonths(months));
    }

    @Transactional
    public Dtos.FdView book(Long userId, Dtos.BookFdRequest req) {
        validate(req.principal(), req.tenureMonths());
        BigDecimal principal = AccountService.money(req.principal());
        BankAccount account = accountService.lockOwned(userId, req.accountId());

        BigDecimal rate = rateFor(req.tenureMonths());
        LocalDate start = accountService.today();

        FixedDeposit fd = new FixedDeposit();
        fd.setUserId(userId);
        fd.setAccountId(account.getId());
        fd.setPrincipal(principal);
        fd.setRatePercent(rate);
        fd.setTenureMonths(req.tenureMonths());
        fd.setMaturityAmount(maturityAmount(principal, rate, req.tenureMonths()));
        fd.setStartDate(start);
        fd.setMaturityDate(start.plusMonths(req.tenureMonths()));
        fd = fdRepository.save(fd);

        accountService.debit(account, Channel.FD_BOOKING, principal,
                "Fixed deposit #" + fd.getId() + " booked (" + req.tenureMonths() + " months @ " + rate + "%)",
                AccountService.newReference(), null);
        return Dtos.FdView.from(fd);
    }

    @Transactional(readOnly = true)
    public List<Dtos.FdView> list(Long userId) {
        return fdRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId).stream().map(Dtos.FdView::from).toList();
    }

    /** Break an FD early (or, if it has already reached maturity, collect it in full). */
    @Transactional
    public Dtos.FdView close(Long userId, Long fdId) {
        FixedDeposit fd = fdRepository.findByIdForUpdate(fdId)
                .filter(f -> f.getUserId().equals(userId))
                .orElseThrow(() -> new NotFoundException("Fixed deposit not found"));
        if (fd.getStatus() != FdStatus.ACTIVE) {
            throw new BadRequestException("This fixed deposit is already closed");
        }
        LocalDate today = accountService.today();
        if (!today.isBefore(fd.getMaturityDate())) {
            return Dtos.FdView.from(payout(fd, FdStatus.MATURED, fd.getMaturityAmount(), Channel.FD_MATURITY));
        }
        long days = ChronoUnit.DAYS.between(fd.getStartDate(), today);
        BigDecimal interest = BigDecimal.ZERO;
        if (days >= MIN_DAYS_FOR_INTEREST) {
            BigDecimal penalisedRate = fd.getRatePercent().subtract(PENALTY_POINTS).max(BigDecimal.ZERO);
            interest = fd.getPrincipal().multiply(penalisedRate).multiply(BigDecimal.valueOf(days))
                    .divide(BigDecimal.valueOf(36500), 2, RoundingMode.HALF_UP);
            BigDecimal ceiling = fd.getMaturityAmount().subtract(fd.getPrincipal());
            interest = interest.min(ceiling);
        }
        return Dtos.FdView.from(payout(fd, FdStatus.CLOSED_PREMATURE, fd.getPrincipal().add(interest), Channel.FD_PREMATURE_CLOSURE));
    }

    /** Called by the scheduler, one FD per transaction so a single failure cannot block the rest. */
    @Transactional
    public void settleMatured(Long fdId) {
        FixedDeposit fd = fdRepository.findByIdForUpdate(fdId).orElse(null);
        if (fd == null || fd.getStatus() != FdStatus.ACTIVE || fd.getMaturityDate().isAfter(accountService.today())) {
            return;
        }
        payout(fd, FdStatus.MATURED, fd.getMaturityAmount(), Channel.FD_MATURITY);
    }

    @Transactional(readOnly = true)
    public List<Long> dueForSettlement() {
        return fdRepository.findByStatusAndMaturityDateLessThanEqual(FdStatus.ACTIVE, accountService.today())
                .stream().map(FixedDeposit::getId).toList();
    }

    private FixedDeposit payout(FixedDeposit fd, FdStatus newStatus, BigDecimal amount, Channel channel) {
        // The FD row is already locked; now lock the account it pays into (fixed order: FD, then account).
        BankAccount account = accountService.lockById(fd.getAccountId());
        String label = newStatus == FdStatus.MATURED ? "matured" : "closed early";
        accountService.credit(account, channel, amount, "Fixed deposit #" + fd.getId() + " " + label,
                AccountService.newReference(), null);
        fd.setStatus(newStatus);
        fd.setPayoutAmount(amount);
        fd.setClosedAt(Instant.now());
        return fdRepository.save(fd);
    }

    private static void validate(BigDecimal principal, int months) {
        if (principal == null || principal.compareTo(new BigDecimal("1000")) < 0) {
            throw new BadRequestException("Minimum fixed deposit is 1,000");
        }
        if (principal.scale() > 2) {
            throw new BadRequestException("Amount can have at most 2 decimal places");
        }
        if (months < 3 || months > 120) {
            throw new BadRequestException("Tenure must be between 3 and 120 months");
        }
    }
}
