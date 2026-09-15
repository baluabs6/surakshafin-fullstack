package com.surakshafin.fraud;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FraudService {

    private final ScamPatternRepository scamPatternRepository;
    private final FraudReportRepository fraudReportRepository;

    public FraudService(ScamPatternRepository scamPatternRepository, FraudReportRepository fraudReportRepository) {
        this.scamPatternRepository = scamPatternRepository;
        this.fraudReportRepository = fraudReportRepository;
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
        // NOTE: in the full architecture this is where a FraudAlertRaised domain event
        // is published to SNS/SQS so notification-service and audit-compliance-service react.
        report = fraudReportRepository.save(report);
        return Dtos.ReportView.from(report);
    }

    public List<Dtos.ReportView> myReports(Long userId) {
        return fraudReportRepository.findByReportedByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(Dtos.ReportView::from).toList();
    }
}
