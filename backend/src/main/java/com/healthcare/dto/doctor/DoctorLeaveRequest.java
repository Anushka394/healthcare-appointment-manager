package com.healthcare.dto.doctor;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DoctorLeaveRequest {

    @NotNull(message = "Leave date is required")
    @FutureOrPresent(message = "Leave date must be today or a future date")
    private LocalDate leaveDate;

    private String reason;
}
