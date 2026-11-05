package com.umc.bscene.domain.oauth.scheduler;

import com.umc.bscene.domain.oauth.service.AppleCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppleCredentialCleanupScheduler {

    private final AppleCredentialService credentials;

    @Scheduled(fixedDelay = 30_000, scheduler = "appleCredentialTaskScheduler")
    public void cleanup() {
        credentials.processDue();
    }
}
