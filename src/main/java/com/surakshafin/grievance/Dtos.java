package com.surakshafin.grievance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.temporal.ChronoUnit;

public class Dtos {

    /** Answers to the decision-tree wizard the Angular UI walks the user through. */
    public record RoutingRequest(
            @NotBlank String issueType,
            @NotBlank @Size(max = 2000) String description,
            boolean alreadyRaisedWithBank,
            boolean bankResponseOverThirtyDays,
            boolean suspectedFraud
    ) {}

    // Feature gap fix: status existed on the entity but there was no endpoint to change it.
    public record UpdateStatusRequest(@NotBlank String status) {}

    public record GrievanceView(
            Long id, String issueType, String description, String routedTo,
            String status, String generatedComplaintText, String createdAt,
            long daysSinceFiled, boolean escalationDue
    ) {
        public static GrievanceView from(Grievance g) {
            long days = ChronoUnit.DAYS.between(g.getCreatedAt(), java.time.Instant.now());
            // New feature: surfaces the same 30-day threshold GrievanceRoutingEngine uses for
            // BANK -> RBI_OMBUDSMAN escalation, so the UI can prompt the user to escalate instead
            // of relying on them to track the date themselves.
            boolean escalationDue = "BANK".equals(g.getRoutedTo())
                    && !"RESOLVED".equals(g.getStatus())
                    && days >= 30;
            return new GrievanceView(g.getId(), g.getIssueType(), g.getDescription(), g.getRoutedTo(),
                    g.getStatus(), g.getGeneratedComplaintText(), g.getCreatedAt().toString(), days, escalationDue);
        }
    }
}
