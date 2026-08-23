package com.healthcare.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_visit_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostVisitRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @Column(name = "clinical_notes", columnDefinition = "TEXT", nullable = false)
    private String clinicalNotes;

    @Column(name = "diagnosis", length = 2000)
    private String diagnosis;

    @Column(name = "follow_up_date")
    private java.time.LocalDate followUpDate;

    // --- LLM Generated Post-visit Summary ---

    @Column(name = "llm_patient_summary", columnDefinition = "TEXT")
    private String llmPatientSummary;

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
