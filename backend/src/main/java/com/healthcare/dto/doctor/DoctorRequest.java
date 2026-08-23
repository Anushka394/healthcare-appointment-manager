package com.healthcare.dto.doctor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class DoctorRequest {

    @NotBlank(message = "Specialisation is required")
    private String specialisation;

    @NotNull(message = "Work start time is required")
    private LocalTime workStartTime;

    @NotNull(message = "Work end time is required")
    private LocalTime workEndTime;

    private Integer slotDurationMinutes = 30;

    private Integer maxPatientsPerDay = 20;

    private String biography;

    private Double consultationFee;

    // Used during admin creation — links to an existing user
    private Long userId;
}
