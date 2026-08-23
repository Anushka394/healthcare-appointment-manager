package com.healthcare.scheduler;

import com.healthcare.entity.Prescription;
import com.healthcare.repository.PrescriptionRepository;
import com.healthcare.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Runs twice daily (8 AM and 8 PM by default) to send medication reminders
 * for all active prescriptions whose reminder window covers today.
 *
 * Cron expression is configurable via app.scheduler.medication-reminder-cron.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MedicationReminderJob {

    private final PrescriptionRepository prescriptionRepository;
    private final EmailService emailService;

    @Scheduled(cron = "${app.scheduler.medication-reminder-cron:0 0 8,20 * * ?}")
    @Transactional(readOnly = true)
    public void sendMedicationReminders() {
        LocalDate today = LocalDate.now();
        List<Prescription> active = prescriptionRepository.findActivePrescriptionsForToday(today);

        if (active.isEmpty()) {
            log.debug("No active medication reminders for {}", today);
            return;
        }

        log.info("Sending medication reminders for {} prescription(s)", active.size());

        for (Prescription p : active) {
            try {
                emailService.sendMedicationReminder(p.getPatient().getUser(), p);
            } catch (Exception e) {
                log.error("Medication reminder failed for prescription {} (patient {}): {}",
                        p.getId(), p.getPatient().getId(), e.getMessage());
            }
        }
    }
}
