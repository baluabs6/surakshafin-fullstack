package com.surakshafin.grievance;

public final class GrievanceRoutingEngine {

    private GrievanceRoutingEngine() {}

    public static String route(Dtos.RoutingRequest req) {
        if (req.suspectedFraud()) {
            return "CYBER_CELL";
        }
        if (!req.alreadyRaisedWithBank()) {
            return "BANK";
        }
        if (req.bankResponseOverThirtyDays()) {
            return "RBI_OMBUDSMAN";
        }
        if ("UNAUTHORIZED_TXN".equals(req.issueType())) {
            return "NPCI";
        }
        return "BANK";
    }

    public static String draftComplaint(Dtos.RoutingRequest req, String routedTo, String userFullName) {
        String recipient = switch (routedTo) {
            case "RBI_OMBUDSMAN" -> "Reserve Bank of India — Integrated Ombudsman Scheme";
            case "NPCI" -> "National Payments Corporation of India (NPCI) Dispute Redressal";
            case "CYBER_CELL" -> "National Cyber Crime Reporting Portal";
            default -> "Bank / Payment Service Provider Grievance Cell";
        };
        return """
                To: %s
                From: %s

                Subject: Complaint regarding %s

                I am writing to report the following issue with my account/transaction:

                %s

                Prior escalation to the bank: %s
                Response received within 30 days: %s

                I request that this matter be investigated and resolved at the earliest,
                and that I be kept informed of the outcome in writing.

                Regards,
                %s
                """.formatted(
                recipient,
                userFullName,
                req.issueType().replace('_', ' ').toLowerCase(),
                req.description(),
                req.alreadyRaisedWithBank() ? "Yes" : "No",
                req.bankResponseOverThirtyDays() ? "Yes" : "No / Not applicable",
                userFullName
        );
    }
}
