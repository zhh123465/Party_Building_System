package com.party.build.warning.job;

import com.party.build.warning.service.WarningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WarningJob {
    private final WarningService warningService;

    @Scheduled(cron = "${app.warning.cron:0 0 2 * * ?}")
    public void generateDaily() {
        int count = warningService.generateAll();
        log.info("warning job finished, new warnings: {}", count);
    }
}