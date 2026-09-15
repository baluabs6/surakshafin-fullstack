package com.surakshafin.grievance;

import com.surakshafin.common.NotFoundException;
import com.surakshafin.identity.IdentityService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GrievanceService {

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
}
