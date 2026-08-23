package com.healthcare.repository;

import com.healthcare.entity.SymptomForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SymptomFormRepository extends JpaRepository<SymptomForm, Long> {
    Optional<SymptomForm> findByAppointmentId(Long appointmentId);
    boolean existsByAppointmentId(Long appointmentId);
}
