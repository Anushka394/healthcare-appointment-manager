package com.healthcare.dto.postvisit;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PostVisitRequest {

    @NotBlank(message = "Clinical notes are required")
    private String clinicalNotes;

    private String diagnosis;

    private LocalDate followUpDate;

    private List<PrescriptionItem> prescriptions;

    @Data
    public static class PrescriptionItem {
        private String medicationName;
        private String dosage;
        private String frequency;
        private Integer remindersPerDay;
        private LocalDate startDate;
        private LocalDate endDate;
        private String instructions;
    }
}
