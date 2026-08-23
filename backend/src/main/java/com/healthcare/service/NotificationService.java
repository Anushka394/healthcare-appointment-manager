package com.healthcare.service;

import com.healthcare.entity.Notification;
import com.healthcare.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Orchestrates the retry mechanism for failed/pending notifications.
 * Called periodically by the scheduler.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    /**
     * Picks up all PENDING and retryable FAILED notifications and attempts re-send.
     */
    @Transactional
    public void processRetryQueue() {
        List<Notification> retryable = notificationRepository.findRetryableNotifications();
        if (retryable.isEmpty()) return;
        log.info("Processing {} retryable notification(s)", retryable.size());
        for (Notification n : retryable) {
            emailService.retrySend(n);
        }
    }

    public List<Notification> getNotificationsForUser(Long userId) {
        return notificationRepository.findByUserId(userId);
    }
}
