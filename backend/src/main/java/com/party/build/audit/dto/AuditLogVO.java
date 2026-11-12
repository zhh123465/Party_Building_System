package com.party.build.audit.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLogVO {
    private Long logId;
    private Long userId;
    private String username;
    private String action;
    private String objectType;
    private Long objectId;
    private String result;
    private String reason;
    private String detail;
    private String ip;
    private String userAgent;
    private LocalDateTime createdAt;
}