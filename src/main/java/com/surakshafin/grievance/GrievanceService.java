package com.surakshafin.grievance;

import com.surakshafin.common.BadRequestException;
import com.surakshafin.common.NotFoundException;
import com.surakshafin.identity.IdentityService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class GrievanceService {

    // Feature gap fix: status previously always sat at its default forever. These are the only
    // valid transitions an operator can apply, mirroring the states already documented on Grievance.
    private static final Set<String> VALID_STATUSES = Set.of("DRAFTED", "FILED", "IN_PROGRESS", "ESCALATED", "RESOLVED");

    private final GrievanceRepository grievanceRepository;
    private final IdentityService identityService;

    public GrievanceService(GrievanceRepository grievanceRepository, IdentityService identityService) {
        this.grievanceRepository = grievanceRepository;
        this.identityService = identityService;
    }

    public Dtos.GrievanceView fileGrievance(Long userId, Dtos.RoutingRequest req) {
        String routedTo = GrievanceRoutingEngine.route(req);
        String userFullName = identityService.getProfile(userId).fullName();
        String complaintText = GrievanceRoutingEngine.draftComplaint(req, routedTo, userFullName);

        Grievance g = new Grievance();
        g.setUserId(userId);
        g.setIssueType(req.issueType());
        g.setDescription(req.description());
        g.setRoutedTo(routedTo);
        g.setStatus("FILED");
        g.setGeneratedComplaintText(complaintText);
        g = grievanceRepository.save(g);
        // NOTE: publishes ComplaintFiled event to audit-compliance-service and
        // notification-service in the full architecture.
        return Dtos.GrievanceView.from(g);
    }

    public List<Dtos.GrievanceView> myGrievances(Long userId) {
        return grievanceRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(Dtos.GrievanceView::from).toList();
    }

    public Dtos.GrievanceView get(Long userId, Long id) {
        Grievance g = grievanceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Grievance not found"));
        if (!g.getUserId().equals(userId)) {
            throw new NotFoundException("Grievance not found");
        }
        return Dtos.GrievanceView.from(g);
    }

    /** Feature gap fix. Admin-only (enforced in SecurityConfig) since a user marking their own
     *  complaint "RESOLVED" would defeat the point of tracking it. */
    public Dtos.GrievanceView updateStatus(Long id, Dtos.UpdateStatusRequest req) {
        if (!VALID_STATUSES.contains(req.status())) {
            throw new BadRequestException("status must be one of " + VALID_STATUSES);
        }
        Grievance g = grievanceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Grievance not found"));
        g.setStatus(req.status());
        g = grievanceRepository.save(g);
        // NOTE: this is where a GrievanceStatusChanged event would notify the filing user.
        return Dtos.GrievanceView.from(g);
    }
}
