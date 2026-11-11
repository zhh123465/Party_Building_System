package com.party.build.process.controller;

import com.party.build.auth.security.UserPrincipal;
import com.party.build.common.api.PageResult;
import com.party.build.common.api.Result;
import com.party.build.process.dto.*;
import com.party.build.process.service.ProcessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/process")
@RequiredArgsConstructor
public class ProcessController {
    private final ProcessService processService;

    @GetMapping("/board")
    @PreAuthorize("hasAnyRole('R1','R2','R3')")
    public Result<PageResult<BoardPersonVO>> board(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "12") long size,
                                                   @RequestParam(required = false) Long branchId,
                                                   @RequestParam(required = false) String stage,
                                                   @RequestParam(required = false) String keyword,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(processService.board(page, Math.min(size, 100), branchId, stage, keyword, principal));
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('R1','R2','R3')")
    public Result<Map<String, Object>> overview(@RequestParam(required = false) Long branchId,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(processService.overview(branchId, principal));
    }

    @GetMapping("/stage-distribution")
    @PreAuthorize("hasAnyRole('R1','R2','R3')")
    public Result<List<StageDistributionVO>> distribution(@RequestParam(required = false) Long branchId,
                                                          @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(processService.stageDistribution(branchId, principal));
    }

    @GetMapping("/persons/{id}/advance-check")
    @PreAuthorize("hasAnyRole('R1','R2')")
    public Result<AdvanceCheckVO> advanceCheck(@PathVariable Long id,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(processService.advanceCheck(id, principal));
    }

    @PostMapping("/persons/{id}/advance")
    @PreAuthorize("hasAnyRole('R1','R2')")
    public Result<AdvanceCheckVO> advance(@PathVariable Long id,
                                          @Valid @RequestBody AdvanceRequest request,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(processService.advance(id, request, principal));
    }

    @PostMapping("/persons/{id}/rollback")
    @PreAuthorize("hasAnyRole('R1','R2')")
    public Result<AdvanceCheckVO> rollback(@PathVariable Long id,
                                           @Valid @RequestBody RollbackRequest request,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(processService.rollback(id, request, principal));
    }
}