package com.healthcare.scheduler;

import com.healthcare.entity.Appointment;
import com.healthcare.repository.AppointmentRepository;
import com.healthcare.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Runs every day at 9 AM. Sends a reminder email for all CONFIRMED appointments
 * scheduled for tomorrow. Marks reminderSent=true to avoid duplicates.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentReminderJob {

    private final AppointmentRepository appointmentRepository;
    private final EmailService emailService;

    @Scheduled(cron = "${app.scheduler.appointment-reminder-cron:0 0 9 * * ?}")
    @Transactional
    public void sendReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Appointment> appointments = appointmentRepository.findUnremindedForDate(tomorrow);

        if (appointments.isEmpty()) {
            log.debug("No appointment reminders to send for {}", tomorrow);
            return;
        }

        log.info("Sending appointment reminders for {} appointment(s) on {}", appointments.size(), tomorrow);

        for (Appointment appt : appointments) {
            try {
                emailService.sendAppointmentReminder(appt);
                appt.setReminderSent(true);
                appointmentRepository.save(appt);
            } catch (Exception e) {
                log.error("Failed to send reminder for appointment {}: {}", appt.getId(), e.getMessage());
            }
        }
    }
}
