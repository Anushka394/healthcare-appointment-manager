package com.healthcare.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "prescriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "medication_name", nullable = false)
    private String medicationName;

    @Column(nullable = false, length = 500)
    private String dosage;

    /**
     * Frequency string, e.g. "Twice daily", "Every 8 hours", "Once at bedtime".
     */
    @Column(nullable = false, length = 200)
    private String frequency;

    /**
     * Number of times per day reminders should be sent.
     */
    @Column(name = "reminders_per_day")
    @Builder.Default
    private Integer remindersPerDay = 1;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(length = 1000)
    private String instructions;

    @Column(name = "reminder_active")
    @Builder.Default
    private Boolean reminderActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
