package com.party.build.audit.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.party.build.audit.domain.AuditLog;
import com.party.build.audit.dto.AuditLogVO;
import com.party.build.audit.mapper.AuditLogMapper;
import com.party.build.common.api.PageResult;
import com.party.build.common.api.Result;
import com.party.build.report.domain.ExportLog;
import com.party.build.report.mapper.ExportLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasRole('R1')")
public class AuditController {
    private final AuditLogMapper auditLogMapper;
    private final ExportLogMapper exportLogMapper;

    @GetMapping("/audit-logs")
    public Result<PageResult<AuditLogVO>> logs(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "20") long size,
                                               @RequestParam(required = false) String username,
                                               @RequestParam(required = false) String action,
                                               @RequestParam(required = false) String objectType,
                                               @RequestParam(required = false)
                                               @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
                                               @RequestParam(required = false)
                                               @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {
        LambdaQueryWrapper<AuditLog> q = new LambdaQueryWrapper<>();
        if (username != null && !username.isBlank()) q.like(AuditLog::getUsername, username);
        if (action != null && !action.isBlank()) q.eq(AuditLog::getAction, action);
        if (objectType != null && !objectType.isBlank()) q.eq(AuditLog::getObjectType, objectType);
        if (start != null) q.ge(AuditLog::getCreatedAt, start);
        if (end != null) q.le(AuditLog::getCreatedAt, end);
        q.orderByDesc(AuditLog::getLogId);
        Page<AuditLog> p = new Page<>(page, size);
        Page<AuditLog> result = auditLogMapper.selectPage(p, q);
        return Result.ok(new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(),
                result.getRecords().stream().map(this::toVO).toList()));
    }

    @GetMapping("/export-logs")
    public Result<PageResult<ExportLog>> exportLogs(@RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "20") long size) {
        Page<ExportLog> p = new Page<>(page, size);
        Page<ExportLog> result = exportLogMapper.selectPage(p, new LambdaQueryWrapper<ExportLog>()
                .orderByDesc(ExportLog::getExportId));
        return Result.ok(new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), result.getRecords()));
    }

    private AuditLogVO toVO(AuditLog log) {
        AuditLogVO vo = new AuditLogVO();
        org.springframework.beans.BeanUtils.copyProperties(log, vo);
        return vo;
    }
}