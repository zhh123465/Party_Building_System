package com.party.build.review.controller;

import com.party.build.auth.security.UserPrincipal;
import com.party.build.common.api.PageResult;
import com.party.build.common.api.Result;
import com.party.build.review.domain.ReviewLog;
import com.party.build.review.dto.PendingReviewVO;
import com.party.build.review.dto.ReviewLogVO;
import com.party.build.review.dto.ReviewRequest;
import com.party.build.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @GetMapping("/reviews/pending")
    @PreAuthorize("hasAnyRole('R1','R2','R3')")
    public Result<PageResult<PendingReviewVO>> pending(@RequestParam(defaultValue = "1") long page,
                                                       @RequestParam(defaultValue = "20") long size,
                                                       @RequestParam(required = false) String stage,
                                                       @RequestParam(required = false) Long branchId,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(reviewService.pending(page, Math.min(size, 100), stage, branchId, principal));
    }

    @GetMapping("/reviews/templates")
    @PreAuthorize("hasAnyRole('R1','R2','R3')")
    public Result<List<String>> templates() {
        return Result.ok(reviewService.templates());
    }

    @PostMapping("/materials/{id}/review")
    @PreAuthorize("hasAnyRole('R1','R2','R3')")
    public Result<ReviewLog> review(@PathVariable Long id,
                                    @Valid @RequestBody ReviewRequest request,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(reviewService.review(id, request, principal));
    }

    @GetMapping("/materials/{id}/reviews")
    public Result<List<ReviewLogVO>> logs(@PathVariable Long id,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(reviewService.logs(id, principal));
    }
}