package com.healthcare.dto.appointment;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class RescheduleRequest {

    @NotNull(message = "New appointment date is required")
    @Future(message = "New appointment date must be in the future")
    private LocalDate newDate;

    @NotNull(message = "New slot start time is required")
    private LocalTime newSlotStartTime;
}
