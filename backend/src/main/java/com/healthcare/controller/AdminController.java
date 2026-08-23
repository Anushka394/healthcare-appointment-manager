package com.healthcare.controller;

import com.healthcare.dto.common.ApiResponse;
import com.healthcare.dto.doctor.DoctorLeaveRequest;
import com.healthcare.dto.doctor.DoctorLeaveResponse;
import com.healthcare.dto.doctor.DoctorRequest;
import com.healthcare.dto.doctor.DoctorResponse;
import com.healthcare.entity.User;
import com.healthcare.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // --- Doctor management ---

    @PostMapping("/doctors")
    public ResponseEntity<ApiResponse<DoctorResponse>> createDoctor(@Valid @RequestBody DoctorRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Doctor profile created", adminService.createDoctor(req)));
    }

    @PutMapping("/doctors/{doctorId}")
    public ResponseEntity<ApiResponse<DoctorResponse>> updateDoctor(
            @PathVariable Long doctorId, @Valid @RequestBody DoctorRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Doctor updated", adminService.updateDoctor(doctorId, req)));
    }

    @GetMapping("/doctors")
    public ResponseEntity<ApiResponse<List<DoctorResponse>>> getAllDoctors() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAllDoctors()));
    }

    @GetMapping("/doctors/{doctorId}")
    public ResponseEntity<ApiResponse<DoctorResponse>> getDoctor(@PathVariable Long doctorId) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getDoctor(doctorId)));
    }

    // --- Leave management ---

    @PostMapping("/doctors/{doctorId}/leaves")
    public ResponseEntity<ApiResponse<DoctorLeaveResponse>> addLeave(
            @PathVariable Long doctorId, @Valid @RequestBody DoctorLeaveRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Leave added", adminService.addLeave(doctorId, req)));
    }

    @DeleteMapping("/leaves/{leaveId}")
    public ResponseEntity<ApiResponse<Void>> removeLeave(@PathVariable Long leaveId) {
        adminService.removeLeave(leaveId);
        return ResponseEntity.ok(ApiResponse.success("Leave removed", null));
    }

    @GetMapping("/doctors/{doctorId}/leaves")
    public ResponseEntity<ApiResponse<List<DoctorLeaveResponse>>> getDoctorLeaves(@PathVariable Long doctorId) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getLeavesForDoctor(doctorId)));
    }

    // --- User management ---

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAllUsers()));
    }

    @PatchMapping("/users/{userId}/activate")
    public ResponseEntity<ApiResponse<Void>> activateUser(@PathVariable Long userId) {
        adminService.toggleUserActive(userId, true);
        return ResponseEntity.ok(ApiResponse.success("User activated", null));
    }

    @PatchMapping("/users/{userId}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(@PathVariable Long userId) {
        adminService.toggleUserActive(userId, false);
        return ResponseEntity.ok(ApiResponse.success("User deactivated", null));
    }
}
