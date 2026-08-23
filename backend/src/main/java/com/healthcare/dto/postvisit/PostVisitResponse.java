package com.healthcare.dto.postvisit;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PostVisitResponse {

    private Long id;
    private Long appointmentId;
    private String clinicalNotes;
    private String diagnosis;
    private LocalDate followUpDate;

    // LLM generated
    private String llmPatientSummary;
    private Boolean llmProcessed;
    private String llmError;

    private List<PrescriptionDto> prescriptions;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class PrescriptionDto {
        private Long id;
        private String medicationName;
        private String dosage;
        private String frequency;
        private Integer remindersPerDay;
        private LocalDate startDate;
        private LocalDate endDate;
        private String instructions;
    }
}
