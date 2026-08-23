package com.healthcare.controller;

import com.healthcare.dto.appointment.AppointmentResponse;
import com.healthcare.dto.common.ApiResponse;
import com.healthcare.dto.doctor.DoctorLeaveRequest;
import com.healthcare.dto.doctor.DoctorLeaveResponse;
import com.healthcare.dto.doctor.DoctorRequest;
import com.healthcare.dto.doctor.DoctorResponse;
import com.healthcare.dto.postvisit.PostVisitRequest;
import com.healthcare.dto.postvisit.PostVisitResponse;
import com.healthcare.entity.Appointment;
import com.healthcare.entity.User;
import com.healthcare.repository.UserRepository;
import com.healthcare.service.AppointmentService;
import com.healthcare.service.DoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/doctor")
@PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;
    private final AppointmentService appointmentService;
    private final UserRepository userRepository;

    // --- Profile ---

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<DoctorResponse>> getProfile(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(doctorService.getMyProfile(resolveUserId(ud))));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<DoctorResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails ud,
            @RequestBody DoctorRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated",
                doctorService.updateMyProfile(resolveUserId(ud), req)));
    }

    // --- Appointments ---

    @GetMapping("/appointments")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getAppointments(
            @AuthenticationPrincipal UserDetails ud) {
        List<AppointmentResponse> list = doctorService.getMyAppointments(resolveUserId(ud))
                .stream().map(appointmentService::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/appointments/date/{date}")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getForDate(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<AppointmentResponse> list = doctorService.getAppointmentsForDate(resolveUserId(ud), date)
                .stream().map(appointmentService::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    // --- Leaves ---

    @PostMapping("/leaves")
    public ResponseEntity<ApiResponse<DoctorLeaveResponse>> requestLeave(
            @AuthenticationPrincipal UserDetails ud,
            @Valid @RequestBody DoctorLeaveRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Leave requested",
                doctorService.requestLeave(resolveUserId(ud), req)));
    }

    @GetMapping("/leaves")
    public ResponseEntity<ApiResponse<List<DoctorLeaveResponse>>> getLeaves(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(doctorService.getMyLeaves(resolveUserId(ud))));
    }

    // --- Post-visit notes ---

    @PostMapping("/appointments/{appointmentId}/post-visit")
    public ResponseEntity<ApiResponse<PostVisitResponse>> submitPostVisit(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable Long appointmentId,
            @Valid @RequestBody PostVisitRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Post-visit notes saved",
                doctorService.submitPostVisit(resolveUserId(ud), appointmentId, req)));
    }

    @GetMapping("/appointments/{appointmentId}/post-visit")
    public ResponseEntity<ApiResponse<PostVisitResponse>> getPostVisit(@PathVariable Long appointmentId) {
        return ResponseEntity.ok(ApiResponse.success(doctorService.getPostVisit(appointmentId)));
    }

    // --- Helper ---

    private Long resolveUserId(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername()).orElseThrow().getId();
    }
}
