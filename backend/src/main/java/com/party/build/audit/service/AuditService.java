package com.party.build.audit.service;

import com.party.build.audit.domain.AuditLog;
import com.party.build.audit.mapper.AuditLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.party.build.auth.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    public void log(UserPrincipal principal, String action, String objectType, Long objectId,
                    String result, String reason, String detail) {
        AuditLog log = new AuditLog();
        if (principal != null) {
            log.setUserId(principal.getUserId());
            log.setUsername(principal.getUsername());
        }
        log.setAction(action);
        log.setObjectType(objectType);
        log.setObjectId(objectId);
        log.setResult(result);
        log.setReason(reason);
        log.setDetail(toJson(detail));
        auditLogMapper.insert(log);
    }

    private String toJson(String detail) {
        if (detail == null || detail.isBlank()) {
            return "{}";
        }
        try {
            objectMapper.readTree(detail);
            return detail;
        } catch (Exception e) {
            try {
                return objectMapper.writeValueAsString(java.util.Map.of("message", detail));
            } catch (Exception ignored) {
                return "{}";
            }
        }
    }
}