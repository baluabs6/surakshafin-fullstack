package com.surakshafin.grievance;

import jakarta.validation.constraints.NotBlank;

public class Dtos {

    /** Answers to the decision-tree wizard the Angular UI walks the user through. */
    public record RoutingRequest(
            @NotBlank String issueType,
            @NotBlank String description,
            boolean alreadyRaisedWithBank,
            boolean bankResponseOverThirtyDays,
            boolean suspectedFraud
    ) {}

    public record GrievanceView(
            Long id, String issueType, String description, String routedTo,
            String status, String generatedComplaintText, String createdAt
    ) {
        public static GrievanceView from(Grievance g) {
            return new GrievanceView(g.getId(), g.getIssueType(), g.getDescription(), g.getRoutedTo(),
                    g.getStatus(), g.getGeneratedComplaintText(), g.getCreatedAt().toString());
        }
    }
}
