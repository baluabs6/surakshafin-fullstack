package com.surakshafin.fraud;

import com.surakshafin.common.ApiResponse;
import com.surakshafin.config.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fraud")
public class FraudController {

    private final FraudService fraudService;

    public FraudController(FraudService fraudService) {
        this.fraudService = fraudService;
    }

    @GetMapping("/patterns")
    public ApiResponse<List<Dtos.ScamPatternView>> patterns(@RequestParam(required = false) String category) {
        return ApiResponse.ok(fraudService.listPatterns(category));
    }

    @PostMapping("/reports")
    public ApiResponse<Dtos.ReportView> report(@Valid @RequestBody Dtos.ReportRequest request) {
        return ApiResponse.ok(fraudService.submitReport(CurrentUser.id(), request), "Report submitted");
    }

    @GetMapping("/reports/mine")
    public ApiResponse<List<Dtos.ReportView>> myReports() {
        return ApiResponse.ok(fraudService.myReports(CurrentUser.id()));
    }
}
