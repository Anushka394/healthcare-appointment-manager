package com.healthcare.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "appointments",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_doctor_date_slot",
                columnNames = {"doctor_id", "appointment_date", "slot_start_time"}
        ),
        indexes = {
                @Index(name = "idx_patient_id", columnList = "patient_id"),
                @Index(name = "idx_doctor_date", columnList = "doctor_id, appointment_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "slot_start_time", nullable = false)
    private LocalTime slotStartTime;

    @Column(name = "slot_end_time", nullable = false)
    private LocalTime slotEndTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.PENDING_SYMPTOMS;

    @Column(name = "cancellation_reason", length = 1000)
    private String cancellationReason;

    // Google Calendar event IDs for patient and doctor
    @Column(name = "patient_calendar_event_id")
    private String patientCalendarEventId;

    @Column(name = "doctor_calendar_event_id")
    private String doctorCalendarEventId;

    @Column(name = "reminder_sent")
    @Builder.Default
    private Boolean reminderSent = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum AppointmentStatus {
        /**
         * Slot is temporarily held during booking flow (max 10 minutes).
         */
        SLOT_HELD,
        /**
         * Patient has not yet submitted symptom form.
         */
        PENDING_SYMPTOMS,
        /**
         * Symptom form submitted; confirmed by system.
         */
        CONFIRMED,
        /**
         * Appointment completed; doctor submitted post-visit notes.
         */
        COMPLETED,
        /**
         * Appointment cancelled by patient, doctor, or admin.
         */
        CANCELLED,
        /**
         * Doctor was marked on leave; appointment was auto-cancelled.
         */
        CANCELLED_DOCTOR_LEAVE
    }
}
