package com.healthcare.service;

import com.healthcare.dto.auth.AuthResponse;
import com.healthcare.dto.auth.LoginRequest;
import com.healthcare.dto.auth.RegisterRequest;
import com.healthcare.entity.Doctor;
import com.healthcare.entity.Patient;
import com.healthcare.entity.User;
import com.healthcare.exception.DuplicateResourceException;
import com.healthcare.repository.DoctorRepository;
import com.healthcare.repository.PatientRepository;
import com.healthcare.repository.UserRepository;
import com.healthcare.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(req.getRole())
                .phoneNumber(req.getPhoneNumber())
                .isActive(true)
                .build();
        user = userRepository.save(user);

        Long profileId = null;

        // Auto-create profile row based on role
        if (req.getRole() == User.Role.PATIENT) {
            Patient patient = Patient.builder().user(user).build();
            patient = patientRepository.save(patient);
            profileId = patient.getId();
        } else if (req.getRole() == User.Role.DOCTOR) {
            // Doctor profile is fully configured by admin later;
            // create a minimal placeholder so foreign keys resolve
            Doctor doctor = Doctor.builder()
                    .user(user)
                    .specialisation("General")
                    .workStartTime(java.time.LocalTime.of(9, 0))
                    .workEndTime(java.time.LocalTime.of(17, 0))
                    .build();
            doctor = doctorRepository.save(doctor);
            profileId = doctor.getId();
        }

        String token = jwtTokenProvider.generateTokenFromEmail(user.getEmail());
        log.info("New user registered: {} [role={}]", user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .profileId(profileId)
                .build();
    }

    public AuthResponse login(LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));

        String token = jwtTokenProvider.generateToken(auth);
        User user = userRepository.findByEmail(req.getEmail()).orElseThrow();

        Long profileId = null;
        if (user.getRole() == User.Role.PATIENT) {
            profileId = patientRepository.findByUserId(user.getId()).map(Patient::getId).orElse(null);
        } else if (user.getRole() == User.Role.DOCTOR) {
            profileId = doctorRepository.findByUserId(user.getId()).map(Doctor::getId).orElse(null);
        }

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .profileId(profileId)
                .build();
    }
}
