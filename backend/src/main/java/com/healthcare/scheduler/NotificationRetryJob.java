package com.healthcare.scheduler;

import com.healthcare.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs every 15 minutes to retry PENDING and FAILED email notifications.
 * Gives up after maxRetries attempts (default 3), marking status as EXHAUSTED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRetryJob {

    private final NotificationService notificationService;

    @Scheduled(fixedDelayString = "${app.scheduler.notification-retry-delay-ms:900000}")
    public void retryFailedNotifications() {
        log.debug("Running notification retry job");
        notificationService.processRetryQueue();
    }
}
