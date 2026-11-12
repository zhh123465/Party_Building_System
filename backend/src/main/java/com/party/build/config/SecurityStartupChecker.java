package com.party.build.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SecurityStartupChecker {
    @Value("${app.jwt.secret}")
    private String jwtSecret;
    @Value("${app.crypto.key}")
    private String cryptoKey;
    @Value("${app.llm.api-key:}")
    private String llmApiKey;
    @Value("${app.llm.mock:false}")
    private boolean mock;

    @EventListener(ApplicationReadyEvent.class)
    public void check() {
        if (jwtSecret == null || jwtSecret.contains("dev-only") || jwtSecret.length() < 32) {
            log.warn("SECURITY: JWT_SECRET 正在使用开发默认值，生产环境必须替换为 32 字节以上随机值");
        }
        if (cryptoKey == null || cryptoKey.contains("dev-only") || cryptoKey.length() < 32) {
            log.warn("SECURITY: CRYPTO_KEY 正在使用开发默认值，生产环境必须替换");
        }
        if (!mock && (llmApiKey == null || llmApiKey.isBlank())) {
            log.warn("SECURITY: LLM_API_KEY 未配置，核验将降级为人工");
        }
        log.info("SECURITY: 当前为 POC 演示配置；正式环境需启用 HTTPS、修改默认密码并限制访问范围");
    }
}