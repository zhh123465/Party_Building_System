package com.party.build.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.party.build.audit.domain.AuditLog;
import com.party.build.audit.mapper.AuditLogMapper;
import com.party.build.auth.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    public void log(UserPrincipal principal, String action, String objectType, Long objectId,
                    String result, String reason, String detail) {
        Long userId = principal == null ? null : principal.getUserId();
        String username = principal == null ? null : principal.getUsername();
        log(userId, username, action, objectType, objectId, result, reason, detail);
    }

    public void logAnonymous(String username, String action, String objectType, Long objectId,
                             String result, String reason, String detail) {
        log(null, username, action, objectType, objectId, result, reason, detail);
    }

    public void log(Long userId, String username, String action, String objectType, Long objectId,
                    String result, String reason, String detail) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setAction(action);
        log.setObjectType(objectType);
        log.setObjectId(objectId);
        log.setResult(result);
        log.setReason(reason);
        log.setDetail(toJson(detail));
        HttpServletRequest request = currentRequest();
        if (request != null) {
            log.setIp(clientIp(request));
            String ua = request.getHeader("User-Agent");
            log.setUserAgent(ua == null ? null : (ua.length() > 250 ? ua.substring(0, 250) : ua));
        }
        auditLogMapper.insert(log);
    }

    private HttpServletRequest currentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs == null ? null : attrs.getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
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