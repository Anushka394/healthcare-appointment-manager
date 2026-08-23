package com.healthcare.service;

import com.healthcare.dto.doctor.DoctorLeaveRequest;
import com.healthcare.dto.doctor.DoctorLeaveResponse;
import com.healthcare.dto.doctor.DoctorRequest;
import com.healthcare.dto.doctor.DoctorResponse;
import com.healthcare.entity.*;
import com.healthcare.exception.BadRequestException;
import com.healthcare.exception.DuplicateResourceException;
import com.healthcare.exception.ResourceNotFoundException;
import com.healthcare.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final DoctorLeaveRepository doctorLeaveRepository;
    private final AppointmentRepository appointmentRepository;
    private final EmailService emailService;

    // ---------------------------------------------------------------
    // Doctor management
    // ---------------------------------------------------------------

    @Transactional
    public DoctorResponse createDoctor(DoctorRequest req) {
        User user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", req.getUserId()));
        if (user.getRole() != User.Role.DOCTOR) {
            throw new BadRequestException("User is not registered with DOCTOR role");
        }
        if (doctorRepository.existsByUserId(user.getId())) {
            throw new DuplicateResourceException("Doctor profile already exists for this user");
        }
        Doctor doctor = Doctor.builder()
                .user(user)
                .specialisation(req.getSpecialisation())
                .workStartTime(req.getWorkStartTime())
                .workEndTime(req.getWorkEndTime())
                .slotDurationMinutes(req.getSlotDurationMinutes() != null ? req.getSlotDurationMinutes() : 30)
                .maxPatientsPerDay(req.getMaxPatientsPerDay() != null ? req.getMaxPatientsPerDay() : 20)
                .biography(req.getBiography())
                .consultationFee(req.getConsultationFee())
                .build();
        doctor = doctorRepository.save(doctor);
        log.info("Doctor profile created for user {}", user.getEmail());
        return toResponse(doctor);
    }

    @Transactional
    public DoctorResponse updateDoctor(Long doctorId, DoctorRequest req) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));
        doctor.setSpecialisation(req.getSpecialisation());
        doctor.setWorkStartTime(req.getWorkStartTime());
        doctor.setWorkEndTime(req.getWorkEndTime());
        if (req.getSlotDurationMinutes() != null) doctor.setSlotDurationMinutes(req.getSlotDurationMinutes());
        if (req.getMaxPatientsPerDay() != null) doctor.setMaxPatientsPerDay(req.getMaxPatientsPerDay());
        if (req.getBiography() != null) doctor.setBiography(req.getBiography());
        if (req.getConsultationFee() != null) doctor.setConsultationFee(req.getConsultationFee());
        return toResponse(doctorRepository.save(doctor));
    }

    public List<DoctorResponse> getAllDoctors() {
        return doctorRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public DoctorResponse getDoctor(Long doctorId) {
        return toResponse(doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId)));
    }

    // ---------------------------------------------------------------
    // Doctor leave management
    // ---------------------------------------------------------------

    @Transactional
    public DoctorLeaveResponse addLeave(Long doctorId, DoctorLeaveRequest req) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));
        if (doctorLeaveRepository.existsByDoctorIdAndLeaveDate(doctorId, req.getLeaveDate())) {
            throw new DuplicateResourceException("Leave already recorded for this date");
        }
        DoctorLeave leave = DoctorLeave.builder()
                .doctor(doctor)
                .leaveDate(req.getLeaveDate())
                .reason(req.getReason())
                .patientsNotified(false)
                .build();
        leave = doctorLeaveRepository.save(leave);

        // Cancel existing appointments on leave date and notify patients
        cancelAppointmentsForLeave(doctor, leave);

        return toLeaveResponse(leave);
    }

    @Transactional
    public void removeLeave(Long leaveId) {
        DoctorLeave leave = doctorLeaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("DoctorLeave", leaveId));
        doctorLeaveRepository.delete(leave);
    }

    public List<DoctorLeaveResponse> getLeavesForDoctor(Long doctorId) {
        return doctorLeaveRepository.findByDoctorId(doctorId).stream()
                .map(this::toLeaveResponse).collect(Collectors.toList());
    }

    // ---------------------------------------------------------------
    // User management
    // ---------------------------------------------------------------

    @Transactional
    public void toggleUserActive(Long userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setIsActive(active);
        userRepository.save(user);
        log.info("User {} set active={}", user.getEmail(), active);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // ---------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------

    private void cancelAppointmentsForLeave(Doctor doctor, DoctorLeave leave) {
        List<Appointment> affected = appointmentRepository
                .findActiveByDoctorAndDate(doctor.getId(), leave.getLeaveDate());
        for (Appointment appt : affected) {
            appt.setStatus(Appointment.AppointmentStatus.CANCELLED_DOCTOR_LEAVE);
            appt.setCancellationReason("Doctor is on leave on " + leave.getLeaveDate());
            appointmentRepository.save(appt);
            try {
                emailService.sendDoctorLeaveCancellation(appt);
            } catch (Exception e) {
                log.error("Failed to send leave cancellation email for appointment {}: {}", appt.getId(), e.getMessage());
            }
        }
        leave.setPatientsNotified(true);
        doctorLeaveRepository.save(leave);
        log.info("Cancelled {} appointment(s) for doctor {} on leave date {}", affected.size(), doctor.getId(), leave.getLeaveDate());
    }

    public DoctorResponse toResponse(Doctor d) {
        return DoctorResponse.builder()
                .id(d.getId())
                .userId(d.getUser().getId())
                .name(d.getUser().getName())
                .email(d.getUser().getEmail())
                .phoneNumber(d.getUser().getPhoneNumber())
                .specialisation(d.getSpecialisation())
                .workStartTime(d.getWorkStartTime())
                .workEndTime(d.getWorkEndTime())
                .slotDurationMinutes(d.getSlotDurationMinutes())
                .maxPatientsPerDay(d.getMaxPatientsPerDay())
                .biography(d.getBiography())
                .consultationFee(d.getConsultationFee())
                .build();
    }

    private DoctorLeaveResponse toLeaveResponse(DoctorLeave l) {
        return DoctorLeaveResponse.builder()
                .id(l.getId())
                .doctorId(l.getDoctor().getId())
                .doctorName(l.getDoctor().getUser().getName())
                .leaveDate(l.getLeaveDate())
                .reason(l.getReason())
                .patientsNotified(l.getPatientsNotified())
                .createdAt(l.getCreatedAt())
                .build();
    }
}
