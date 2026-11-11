package com.party.build.warning.controller;

import com.party.build.auth.security.UserPrincipal;
import com.party.build.common.api.PageResult;
import com.party.build.common.api.Result;
import com.party.build.warning.domain.TimeRule;
import com.party.build.warning.dto.*;
import com.party.build.warning.service.WarningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WarningController {
    private final WarningService warningService;

    @GetMapping("/time-rules")
    @PreAuthorize("hasRole('R1')")
    public Result<List<TimeRule>> timeRules() {
        return Result.ok(warningService.timeRules());
    }

    @PutMapping("/time-rules/{id}")
    @PreAuthorize("hasRole('R1')")
    public Result<TimeRule> updateTimeRule(@PathVariable Long id,
                                           @RequestBody TimeRuleRequest request) {
        return Result.ok(warningService.updateTimeRule(id, request));
    }

    @GetMapping("/warnings")
    @PreAuthorize("hasAnyRole('R1','R2','R3')")
    public Result<PageResult<WarningVO>> warnings(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "20") long size,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) String level,
                                                  @RequestParam(required = false) Long branchId,
                                                  @RequestParam(required = false) String stage,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(warningService.list(page, Math.min(size, 100), status, level, branchId, stage, principal));
    }

    @GetMapping("/warnings/summary")
    @PreAuthorize("hasAnyRole('R1','R2','R3')")
    public Result<Map<String, Object>> summary(@RequestParam(required = false) Long branchId,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(warningService.summary(branchId, principal));
    }

    @PutMapping("/warnings/{id}/handle")
    @PreAuthorize("hasAnyRole('R1','R2','R3')")
    public Result<Void> handle(@PathVariable Long id,
                               @Valid @RequestBody WarningHandleRequest request,
                               @AuthenticationPrincipal UserPrincipal principal) {
        warningService.handle(id, request, principal);
        return Result.ok();
    }

    @PostMapping("/warnings/generate")
    @PreAuthorize("hasRole('R1')")
    public Result<Map<String, Integer>> generate() {
        return Result.ok(Map.of("created", warningService.generateAll()));
    }

    @GetMapping("/schedule/calendar")
    @PreAuthorize("hasAnyRole('R1','R2','R3')")
    public Result<List<CalendarItemVO>> calendar(@RequestParam(required = false) Integer year,
                                                 @RequestParam(required = false) Long branchId,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        int y = year == null ? LocalDate.now().getYear() : year;
        return Result.ok(warningService.calendar(y, branchId, principal));
    }

    @PostMapping("/schedule/reverse")
    @PreAuthorize("hasAnyRole('R1','R2','R3')")
    public Result<ScheduleResponse> reverse(@Valid @RequestBody ReverseScheduleRequest request,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(warningService.reverse(request, principal));
    }
}