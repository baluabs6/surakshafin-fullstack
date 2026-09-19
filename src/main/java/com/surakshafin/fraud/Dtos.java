package com.surakshafin.fraud;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public class Dtos {

    public record ScamPatternView(Long id, String title, String description, String category, String severity, int confirmedCount) {
        public static ScamPatternView from(ScamPattern p) {
            return new ScamPatternView(p.getId(), p.getTitle(), p.getDescription(), p.getCategory(), p.getSeverity(), p.getConfirmedCount());
        }
    }

    public record ReportRequest(
            @NotBlank String category,
            @NotBlank @Size(max = 2000) String details,
            @Size(max = 100) String suspectUpiId,
            @Size(max = 20) String suspectPhoneNumber
    ) {}

    public record ReportView(Long id, String category, String details, String status, String createdAt) {
        public static ReportView from(FraudReport r) {
            return new ReportView(r.getId(), r.getCategory(), r.getDetails(), r.getStatus(), r.getCreatedAt().toString());
        }
    }

    // Feature gap fix: FraudReport.status existed but nothing ever moved it off "SUBMITTED".
    public record UpdateStatusRequest(@NotBlank String status) {}

    // --- new feature: pre-transaction safety check ---

    public record PreTransactionCheckRequest(
            String payeeUpiId,
            String payeePhoneNumber,
            @NotNull @Positive BigDecimal amount
    ) {}

    public record PreTransactionCheckResponse(
            String riskLevel, // LOW, MEDIUM, HIGH
            List<String> reasons,
            List<String> checklistPrompts,
            boolean suggestCoolingOff,
            int coolingOffSeconds
    ) {}

    // --- new feature: trusted payees ---

    public record TrustedPayeeRequest(
            @NotBlank @Size(max = 100) String payeeIdentifier,
            @Size(max = 60) String label
    ) {}

    public record TrustedPayeeView(Long id, String payeeIdentifier, String label, String createdAt) {
        public static TrustedPayeeView from(TrustedPayee p) {
            return new TrustedPayeeView(p.getId(), p.getPayeeIdentifier(), p.getLabel(), p.getCreatedAt().toString());
        }
    }
}
