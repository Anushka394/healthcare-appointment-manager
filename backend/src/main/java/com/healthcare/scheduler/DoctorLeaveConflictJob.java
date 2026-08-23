package com.healthcare.scheduler;

import com.healthcare.entity.Appointment;
import com.healthcare.entity.DoctorLeave;
import com.healthcare.repository.AppointmentRepository;
import com.healthcare.repository.DoctorLeaveRepository;
import com.healthcare.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Safety net: scans for any doctor leave records where affected patients
 * have not yet been notified (patientsNotified = false) and handles them.
 *
 * This covers edge cases where the AdminService notification step failed
 * mid-transaction. Runs every 30 minutes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DoctorLeaveConflictJob {

    private final DoctorLeaveRepository doctorLeaveRepository;
    private final AppointmentRepository appointmentRepository;
    private final EmailService emailService;

    @Scheduled(fixedDelayString = "${app.scheduler.leave-conflict-check-delay-ms:1800000}")
    @Transactional
    public void processUnnotifiedLeaves() {
        List<DoctorLeave> unnotified = doctorLeaveRepository.findUnnotifiedLeaves();
        if (unnotified.isEmpty()) return;

        log.info("Processing {} unnotified doctor leave record(s)", unnotified.size());

        for (DoctorLeave leave : unnotified) {
            List<Appointment> affected = appointmentRepository
                    .findActiveByDoctorAndDate(leave.getDoctor().getId(), leave.getLeaveDate());

            for (Appointment appt : affected) {
                try {
                    appt.setStatus(Appointment.AppointmentStatus.CANCELLED_DOCTOR_LEAVE);
                    appt.setCancellationReason("Doctor is on leave on " + leave.getLeaveDate());
                    appointmentRepository.save(appt);
                    emailService.sendDoctorLeaveCancellation(appt);
                } catch (Exception e) {
                    log.error("Leave conflict notification failed for appointment {}: {}", appt.getId(), e.getMessage());
                }
            }

            leave.setPatientsNotified(true);
            doctorLeaveRepository.save(leave);
            log.info("Notified {} patient(s) for doctor {} leave on {}",
                    affected.size(), leave.getDoctor().getId(), leave.getLeaveDate());
        }
    }
}
