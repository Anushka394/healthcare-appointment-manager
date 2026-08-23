package com.healthcare.service;

import com.healthcare.entity.Appointment;
import com.healthcare.entity.Notification;
import com.healthcare.entity.Notification.NotificationStatus;
import com.healthcare.entity.Notification.NotificationType;
import com.healthcare.entity.Prescription;
import com.healthcare.entity.User;
import com.healthcare.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Handles all outbound email.
 * Every send attempt is persisted in the notifications table first,
 * so the scheduler can retry failed sends independently.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;

    @Value("${app.mail.from}")
    private String fromAddress;

    // ---------------------------------------------------------------
    // Public helpers — create a DB record then fire async send
    // ---------------------------------------------------------------

    public void sendBookingConfirmation(Appointment appointment) {
        String patientSubject = "Appointment Confirmed – " + appointment.getAppointmentDate();
        String patientBody = buildBookingConfirmationBody(appointment, false);
        queueAndSend(appointment.getPatient().getUser(), appointment,
                NotificationType.BOOKING_CONFIRMATION, patientSubject, patientBody);

        String doctorSubject = "New Appointment – " + appointment.getPatient().getUser().getName()
                + " on " + appointment.getAppointmentDate();
        String doctorBody = buildBookingConfirmationBody(appointment, true);
        queueAndSend(appointment.getDoctor().getUser(), appointment,
                NotificationType.BOOKING_CONFIRMATION, doctorSubject, doctorBody);
    }

    public void sendAppointmentReminder(Appointment appointment) {
        String subject = "Reminder: Appointment Tomorrow – " + appointment.getAppointmentDate();
        String patientBody = buildReminderBody(appointment, false);
        queueAndSend(appointment.getPatient().getUser(), appointment,
                NotificationType.APPOINTMENT_REMINDER, subject, patientBody);

        String doctorBody = buildReminderBody(appointment, true);
        queueAndSend(appointment.getDoctor().getUser(), appointment,
                NotificationType.APPOINTMENT_REMINDER, subject, doctorBody);
    }

    public void sendCancellationNotice(Appointment appointment, String reason) {
        String subject = "Appointment Cancelled – " + appointment.getAppointmentDate();
        String body = buildCancellationBody(appointment, reason);
        queueAndSend(appointment.getPatient().getUser(), appointment,
                NotificationType.CANCELLATION, subject, body);
        queueAndSend(appointment.getDoctor().getUser(), appointment,
                NotificationType.CANCELLATION, subject, body);
    }

    public void sendDoctorLeaveCancellation(Appointment appointment) {
        String subject = "Appointment Cancelled – Doctor on Leave – " + appointment.getAppointmentDate();
        String body = buildDoctorLeaveCancellationBody(appointment);
        queueAndSend(appointment.getPatient().getUser(), appointment,
                NotificationType.DOCTOR_LEAVE_CANCELLATION, subject, body);
    }

    public void sendPostVisitSummary(Appointment appointment, String summary) {
        String subject = "Your Visit Summary – " + appointment.getAppointmentDate();
        String body = buildPostVisitBody(appointment, summary);
        queueAndSend(appointment.getPatient().getUser(), appointment,
                NotificationType.POST_VISIT_SUMMARY, subject, body);
    }

    public void sendMedicationReminder(User patient, Prescription prescription) {
        String subject = "Medication Reminder – " + prescription.getMedicationName();
        String body = buildMedicationReminderBody(patient.getName(), prescription);
        queueAndSend(patient, null, NotificationType.MEDICATION_REMINDER, subject, body);
    }

    // ---------------------------------------------------------------
    // Retry — called by the scheduler for PENDING/FAILED notifications
    // ---------------------------------------------------------------

    @Async("asyncExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void retrySend(Notification notification) {
        if (notification.getRetryCount() >= notification.getMaxRetries()) {
            notification.setStatus(NotificationStatus.EXHAUSTED);
            notificationRepository.save(notification);
            return;
        }
        doSend(notification);
    }

    // ---------------------------------------------------------------
    // Internal
    // ---------------------------------------------------------------

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void queueAndSend(User recipient, Appointment appointment,
                              NotificationType type, String subject, String body) {
        Notification notification = Notification.builder()
                .user(recipient)
                .appointment(appointment)
                .type(type)
                .subject(subject)
                .body(body)
                .recipientEmail(recipient.getEmail())
                .status(NotificationStatus.PENDING)
                .build();
        notificationRepository.save(notification);
        doSend(notification);
    }

    @Async("asyncExecutor")
    public void doSend(Notification notification) {
        notification.setLastAttemptAt(LocalDateTime.now());
        notification.setRetryCount(notification.getRetryCount() + 1);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(notification.getRecipientEmail());
            helper.setSubject(notification.getSubject());
            helper.setText(notification.getBody(), true);
            mailSender.send(message);

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            log.info("Email sent to {} [type={}]", notification.getRecipientEmail(), notification.getType());
        } catch (MessagingException e) {
            log.error("Email send failed to {} (attempt {}): {}",
                    notification.getRecipientEmail(), notification.getRetryCount(), e.getMessage());
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            if (notification.getRetryCount() >= notification.getMaxRetries()) {
                notification.setStatus(NotificationStatus.EXHAUSTED);
            }
        }
        notificationRepository.save(notification);
    }

    // ---------------------------------------------------------------
    // Email body builders
    // ---------------------------------------------------------------

    private String buildBookingConfirmationBody(Appointment appt, boolean isDoctor) {
        String recipient = isDoctor
                ? "Dr. " + appt.getDoctor().getUser().getName()
                : appt.getPatient().getUser().getName();
        return html(
            "<h2>Appointment Confirmed</h2>" +
            "<p>Dear " + recipient + ",</p>" +
            "<p>Your appointment has been confirmed with the following details:</p>" +
            appointmentTable(appt) +
            "<p>Please arrive 10 minutes early.</p>" +
            footer()
        );
    }

    private String buildReminderBody(Appointment appt, boolean isDoctor) {
        String recipient = isDoctor
                ? "Dr. " + appt.getDoctor().getUser().getName()
                : appt.getPatient().getUser().getName();
        return html(
            "<h2>Appointment Reminder</h2>" +
            "<p>Dear " + recipient + ",</p>" +
            "<p>This is a reminder about your upcoming appointment:</p>" +
            appointmentTable(appt) +
            footer()
        );
    }

    private String buildCancellationBody(Appointment appt, String reason) {
        String reasonHtml = (reason != null && !reason.isBlank())
                ? "<p><strong>Reason:</strong> " + reason + "</p>" : "";
        return html(
            "<h2>Appointment Cancelled</h2>" +
            "<p>Your appointment scheduled for <strong>" + appt.getAppointmentDate() +
            "</strong> at <strong>" + appt.getSlotStartTime() + "</strong> has been cancelled.</p>" +
            reasonHtml +
            "<p>Please book a new appointment at your convenience.</p>" +
            footer()
        );
    }

    private String buildDoctorLeaveCancellationBody(Appointment appt) {
        return html(
            "<h2>Appointment Cancelled – Doctor Unavailable</h2>" +
            "<p>Dear " + appt.getPatient().getUser().getName() + ",</p>" +
            "<p>We regret to inform you that your appointment on <strong>" +
            appt.getAppointmentDate() + "</strong> with <strong>Dr. " +
            appt.getDoctor().getUser().getName() +
            "</strong> has been cancelled because the doctor is on leave that day.</p>" +
            "<p>We apologise for the inconvenience. Please book a new appointment.</p>" +
            footer()
        );
    }

    private String buildPostVisitBody(Appointment appt, String summary) {
        return html(
            "<h2>Your Visit Summary</h2>" +
            "<p>Dear " + appt.getPatient().getUser().getName() + ",</p>" +
            "<p>Here is a summary of your visit on <strong>" + appt.getAppointmentDate() +
            "</strong> with <strong>Dr. " + appt.getDoctor().getUser().getName() + "</strong>:</p>" +
            "<div style='background:#f9f9f9;padding:16px;border-radius:6px;'>" +
            summary.replace("\n", "<br/>") +
            "</div>" +
            footer()
        );
    }

    private String buildMedicationReminderBody(String patientName, Prescription p) {
        return html(
            "<h2>Medication Reminder</h2>" +
            "<p>Dear " + patientName + ",</p>" +
            "<p>This is your reminder to take your medication:</p>" +
            "<table style='border-collapse:collapse;width:100%'>" +
            "<tr><td style='padding:8px;border:1px solid #ddd'><strong>Medication</strong></td>" +
            "<td style='padding:8px;border:1px solid #ddd'>" + p.getMedicationName() + "</td></tr>" +
            "<tr><td style='padding:8px;border:1px solid #ddd'><strong>Dosage</strong></td>" +
            "<td style='padding:8px;border:1px solid #ddd'>" + p.getDosage() + "</td></tr>" +
            "<tr><td style='padding:8px;border:1px solid #ddd'><strong>Frequency</strong></td>" +
            "<td style='padding:8px;border:1px solid #ddd'>" + p.getFrequency() + "</td></tr>" +
            (p.getInstructions() != null ? "<tr><td style='padding:8px;border:1px solid #ddd'>" +
            "<strong>Instructions</strong></td><td style='padding:8px;border:1px solid #ddd'>" +
            p.getInstructions() + "</td></tr>" : "") +
            "</table>" +
            footer()
        );
    }

    private String appointmentTable(Appointment appt) {
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("hh:mm a");
        return "<table style='border-collapse:collapse;width:100%'>" +
               "<tr><td style='padding:8px;border:1px solid #ddd'><strong>Doctor</strong></td>" +
               "<td style='padding:8px;border:1px solid #ddd'>Dr. " + appt.getDoctor().getUser().getName() + "</td></tr>" +
               "<tr><td style='padding:8px;border:1px solid #ddd'><strong>Specialisation</strong></td>" +
               "<td style='padding:8px;border:1px solid #ddd'>" + appt.getDoctor().getSpecialisation() + "</td></tr>" +
               "<tr><td style='padding:8px;border:1px solid #ddd'><strong>Patient</strong></td>" +
               "<td style='padding:8px;border:1px solid #ddd'>" + appt.getPatient().getUser().getName() + "</td></tr>" +
               "<tr><td style='padding:8px;border:1px solid #ddd'><strong>Date</strong></td>" +
               "<td style='padding:8px;border:1px solid #ddd'>" + appt.getAppointmentDate() + "</td></tr>" +
               "<tr><td style='padding:8px;border:1px solid #ddd'><strong>Time</strong></td>" +
               "<td style='padding:8px;border:1px solid #ddd'>" +
               appt.getSlotStartTime().format(timeFmt) + " – " + appt.getSlotEndTime().format(timeFmt) + "</td></tr>" +
               "</table>";
    }

    private String html(String content) {
        return "<html><body style='font-family:Arial,sans-serif;color:#333;max-width:600px;margin:auto'>"
                + content + "</body></html>";
    }

    private String footer() {
        return "<br/><hr/><p style='font-size:12px;color:#888'>Healthcare Appointment Manager – " +
               "This is an automated message, please do not reply.</p>";
    }
}
