package com.surakshafin.fraud;

import jakarta.validation.constraints.NotBlank;

public class Dtos {

    public record ScamPatternView(Long id, String title, String description, String category, String severity) {
        public static ScamPatternView from(ScamPattern p) {
            return new ScamPatternView(p.getId(), p.getTitle(), p.getDescription(), p.getCategory(), p.getSeverity());
        }
    }

    public record ReportRequest(
            @NotBlank String category,
            @NotBlank String details,
            String suspectUpiId,
            String suspectPhoneNumber
    ) {}

    public record ReportView(Long id, String category, String details, String status, String createdAt) {
        public static ReportView from(FraudReport r) {
            return new ReportView(r.getId(), r.getCategory(), r.getDetails(), r.getStatus(), r.getCreatedAt().toString());
        }
    }
}
