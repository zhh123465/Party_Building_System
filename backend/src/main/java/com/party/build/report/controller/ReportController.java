package com.party.build.report.controller;

import com.party.build.auth.security.UserPrincipal;
import com.party.build.common.api.Result;
import com.party.build.report.dto.*;
import com.party.build.report.service.ReportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('R1','R2','R3')")
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/stage-count")
    public Result<List<StageCountVO>> stageCount(@RequestParam(required = false) Long branchId,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(reportService.stageCount(branchId, principal));
    }

    @GetMapping("/overview")
    public Result<ReportOverviewVO> overview(@RequestParam(required = false) Long branchId,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(reportService.overview(branchId, principal));
    }

    @GetMapping("/completion")
    public Result<ReportOverviewVO> completion(@RequestParam(required = false) Long branchId,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(reportService.overview(branchId, principal));
    }

    @GetMapping("/compliance")
    public Result<List<ComplianceVO>> compliance(@RequestParam(required = false) Long branchId,
                                                 @RequestParam(required = false) String stage,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        List<ComplianceVO> list = reportService.complianceList(branchId, principal);
        if (stage != null && !stage.isBlank()) {
            list = list.stream().filter(c -> stage.equals(c.getStage())).toList();
        }
        return Result.ok(list);
    }

    @GetMapping("/compliance/heatmap")
    public Result<List<HeatmapCellVO>> heatmap(@RequestParam(required = false) Long branchId,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(reportService.heatmap(branchId, principal));
    }

    @GetMapping("/export")
    public void export(@RequestParam(defaultValue = "PERSON") String type,
                       @RequestParam(required = false) Long branchId,
                       @AuthenticationPrincipal UserPrincipal principal,
                       HttpServletResponse response) {
        reportService.export(type, branchId, principal, response);
    }
}