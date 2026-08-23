package com.healthcare.scheduler;

import com.healthcare.repository.SlotHoldRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Runs every 5 minutes to purge expired slot holds.
 * Cron is configurable via app.scheduler.slot-hold-cleanup-cron.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlotHoldCleanupJob {

    private final SlotHoldRepository slotHoldRepository;

    @Scheduled(cron = "${app.scheduler.slot-hold-cleanup-cron:0 */5 * * * ?}")
    @Transactional
    public void cleanExpiredHolds() {
        int deleted = slotHoldRepository.deleteExpiredHolds(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired slot hold(s)", deleted);
        }
    }
}
