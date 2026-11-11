package com.party.build.audit.controller;

import com.party.build.audit.domain.Message;
import com.party.build.audit.service.MessageService;
import com.party.build.auth.security.UserPrincipal;
import com.party.build.common.api.PageResult;
import com.party.build.common.api.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {
    private final MessageService messageService;

    @GetMapping
    public Result<PageResult<Message>> list(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "20") long size,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(messageService.list(principal.getUserId(), page, Math.min(size, 100)));
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Long>> unreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(Map.of("count", messageService.unreadCount(principal.getUserId())));
    }

    @PutMapping("/{id}/read")
    public Result<Void> read(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        messageService.markRead(id, principal.getUserId());
        return Result.ok();
    }

    @PutMapping("/read-all")
    public Result<Void> readAll(@AuthenticationPrincipal UserPrincipal principal) {
        messageService.markAllRead(principal.getUserId());
        return Result.ok();
    }
}