package com.healthcare.dto.appointment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableSlotsResponse {

    private Long doctorId;
    private LocalDate date;
    private List<SlotInfo> availableSlots;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlotInfo {
        private LocalTime startTime;
        private LocalTime endTime;
        private boolean available;
    }
}
