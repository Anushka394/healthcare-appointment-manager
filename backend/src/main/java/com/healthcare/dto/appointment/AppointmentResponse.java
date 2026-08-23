package com.healthcare.dto.appointment;

import com.healthcare.entity.Appointment;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class AppointmentResponse {

    private Long id;
    private Long patientId;
    private String patientName;
    private String patientEmail;
    private Long doctorId;
    private String doctorName;
    private String doctorSpecialisation;
    private LocalDate appointmentDate;
    private LocalTime slotStartTime;
    private LocalTime slotEndTime;
    private Appointment.AppointmentStatus status;
    private String cancellationReason;
    private Boolean reminderSent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
