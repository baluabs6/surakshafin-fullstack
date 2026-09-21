package com.surakshafin.grievance;

import com.surakshafin.common.BadRequestException;
import com.surakshafin.common.NotFoundException;
import com.surakshafin.identity.IdentityService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class GrievanceService {

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

    public Dtos.GrievanceView updateStatus(Long id, Dtos.UpdateStatusRequest req) {
        if (!VALID_STATUSES.contains(req.status())) {
            throw new BadRequestException("status must be one of " + VALID_STATUSES);
        }
        Grievance g = grievanceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Grievance not found"));
        g.setStatus(req.status());
        g = grievanceRepository.save(g);
        return Dtos.GrievanceView.from(g);
    }
}
