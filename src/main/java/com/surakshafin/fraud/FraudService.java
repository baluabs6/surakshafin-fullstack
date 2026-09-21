package com.surakshafin.fraud;

import com.surakshafin.common.BadRequestException;
import com.surakshafin.common.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class FraudService {

    private static final Set<String> VALID_STATUSES = Set.of("SUBMITTED", "FORWARDED_TO_NPCI", "FORWARDED_TO_CYBER_CELL", "RESOLVED");

    private static final List<String> NEW_PAYEE_CHECKLIST = List.of(
            "Double-check the payee's name shown on the confirmation screen matches who you expect",
            "If someone asked you to pay to \"receive\" a refund or prize, that's always a scam",
            "When in doubt, call the person/business on a number you already know — not one they just gave you"
    );

    private final ScamPatternRepository scamPatternRepository;
    private final FraudReportRepository fraudReportRepository;
    private final TrustedPayeeRepository trustedPayeeRepository;
    private final BigDecimal newPayeeAmountThreshold;
    private final int coolingOffSeconds;

    public FraudService(ScamPatternRepository scamPatternRepository,
                         FraudReportRepository fraudReportRepository,
                         TrustedPayeeRepository trustedPayeeRepository,
                         @Value("${surakshafin.fraud.new-payee-amount-threshold}") BigDecimal newPayeeAmountThreshold,
                         @Value("${surakshafin.fraud.cooling-off-seconds}") int coolingOffSeconds) {
        this.scamPatternRepository = scamPatternRepository;
        this.fraudReportRepository = fraudReportRepository;
        this.trustedPayeeRepository = trustedPayeeRepository;
        this.newPayeeAmountThreshold = newPayeeAmountThreshold;
        this.coolingOffSeconds = coolingOffSeconds;
    }

    public List<Dtos.ScamPatternView> listPatterns(String category) {
        List<ScamPattern> patterns = (category == null || category.isBlank())
                ? scamPatternRepository.findAll()
                : scamPatternRepository.findByCategory(category);
        return patterns.stream().map(Dtos.ScamPatternView::from).toList();
    }

    public Dtos.ReportView submitReport(Long userId, Dtos.ReportRequest req) {
        FraudReport report = new FraudReport();
        report.setReportedByUserId(userId);
        report.setCategory(req.category());
        report.setDetails(req.details());
        report.setSuspectUpiId(req.suspectUpiId());
        report.setSuspectPhoneNumber(req.suspectPhoneNumber());
        report = fraudReportRepository.save(report);
        return Dtos.ReportView.from(report);
    }

    public List<Dtos.ReportView> myReports(Long userId) {
        return fraudReportRepository.findByReportedByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(Dtos.ReportView::from).toList();
    }

    public Dtos.ReportView updateStatus(Long id, Dtos.UpdateStatusRequest req) {
        if (!VALID_STATUSES.contains(req.status())) {
            throw new BadRequestException("status must be one of " + VALID_STATUSES);
        }
        FraudReport report = fraudReportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Fraud report not found"));
        report.setStatus(req.status());
        report = fraudReportRepository.save(report);
        return Dtos.ReportView.from(report);
    }

    public Dtos.PreTransactionCheckResponse checkBeforeTransaction(Long userId, Dtos.PreTransactionCheckRequest req) {
        String identifier = req.payeeUpiId() != null && !req.payeeUpiId().isBlank()
                ? req.payeeUpiId() : req.payeePhoneNumber();

        if (identifier != null && trustedPayeeRepository.findByUserIdAndPayeeIdentifier(userId, identifier).isPresent()) {
            return new Dtos.PreTransactionCheckResponse("LOW", List.of("Trusted payee"), List.of(), false, 0);
        }

        List<String> reasons = new ArrayList<>();
        boolean highRisk = false;

        if (req.payeeUpiId() != null && !req.payeeUpiId().isBlank()
                && fraudReportRepository.countBySuspectUpiId(req.payeeUpiId()) > 0) {
            highRisk = true;
            reasons.add("This UPI ID has already been reported as a suspect on SuRakshaFin");
        }
        if (req.payeePhoneNumber() != null && !req.payeePhoneNumber().isBlank()
                && fraudReportRepository.countBySuspectPhoneNumber(req.payeePhoneNumber()) > 0) {
            highRisk = true;
            reasons.add("This phone number has already been reported as a suspect on SuRakshaFin");
        }

        if (highRisk) {
            return new Dtos.PreTransactionCheckResponse("HIGH", reasons, NEW_PAYEE_CHECKLIST, true, coolingOffSeconds);
        }

        boolean newPayeeLargeAmount = identifier != null && req.amount().compareTo(newPayeeAmountThreshold) >= 0;
        if (newPayeeLargeAmount) {
            reasons.add("First time paying this amount to a payee not on your trusted list");
            return new Dtos.PreTransactionCheckResponse("MEDIUM", reasons, NEW_PAYEE_CHECKLIST, true, coolingOffSeconds);
        }

        return new Dtos.PreTransactionCheckResponse("LOW", List.of(), List.of(), false, 0);
    }

    public Dtos.TrustedPayeeView addTrustedPayee(Long userId, Dtos.TrustedPayeeRequest req) {
        trustedPayeeRepository.findByUserIdAndPayeeIdentifier(userId, req.payeeIdentifier())
                .ifPresent(p -> { throw new BadRequestException("This payee is already trusted"); });
        TrustedPayee payee = new TrustedPayee();
        payee.setUserId(userId);
        payee.setPayeeIdentifier(req.payeeIdentifier());
        payee.setLabel(req.label());
        payee = trustedPayeeRepository.save(payee);
        return Dtos.TrustedPayeeView.from(payee);
    }

    public List<Dtos.TrustedPayeeView> listTrustedPayees(Long userId) {
        return trustedPayeeRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(Dtos.TrustedPayeeView::from).toList();
    }

    public void removeTrustedPayee(Long userId, Long id) {
        trustedPayeeRepository.deleteByUserIdAndId(userId, id);
    }

    public Dtos.ScamPatternView confirmPattern(Long patternId) {
        ScamPattern pattern = scamPatternRepository.findById(patternId)
                .orElseThrow(() -> new NotFoundException("Scam pattern not found"));
        pattern.setConfirmedCount(pattern.getConfirmedCount() + 1);
        pattern = scamPatternRepository.save(pattern);
        return Dtos.ScamPatternView.from(pattern);
    }
}
