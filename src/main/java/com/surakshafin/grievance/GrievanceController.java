package com.surakshafin.grievance;

import com.surakshafin.common.ApiResponse;
import com.surakshafin.config.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/grievances")
public class GrievanceController {

    private final GrievanceService grievanceService;

    public GrievanceController(GrievanceService grievanceService) {
        this.grievanceService = grievanceService;
    }

    @PostMapping
    public ApiResponse<Dtos.GrievanceView> file(@Valid @RequestBody Dtos.RoutingRequest request) {
        return ApiResponse.ok(grievanceService.fileGrievance(CurrentUser.id(), request), "Grievance filed");
    }

    @GetMapping("/mine")
    public ApiResponse<List<Dtos.GrievanceView>> mine() {
        return ApiResponse.ok(grievanceService.myGrievances(CurrentUser.id()));
    }

    @GetMapping("/{id}")
    public ApiResponse<Dtos.GrievanceView> get(@PathVariable Long id) {
        return ApiResponse.ok(grievanceService.get(CurrentUser.id(), id));
    }
}
