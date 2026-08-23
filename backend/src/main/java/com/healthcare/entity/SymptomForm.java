package com.healthcare.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "symptom_forms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SymptomForm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @Column(nullable = false, length = 5000)
    private String symptoms;

    @Column(name = "symptom_duration")
    private String symptomDuration;

    @Column(name = "severity_self_reported", length = 50)
    private String severitySelfReported;

    @Column(name = "current_medications", length = 2000)
    private String currentMedications;

    // --- LLM Generated Pre-visit Summary ---

    @Column(name = "llm_urgency_level", length = 20)
    private String llmUrgencyLevel;

    @Column(name = "llm_chief_complaint", length = 1000)
    private String llmChiefComplaint;

    @Column(name = "llm_suggested_questions", length = 3000)
    private String llmSuggestedQuestions;

    @Column(name = "llm_raw_response", columnDefinition = "TEXT")
    private String llmRawResponse;

    @Column(name = "llm_processed")
    @Builder.Default
    private Boolean llmProcessed = false;

    @Column(name = "llm_error")
    private String llmError;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
