package com.healthcare.repository;

import com.healthcare.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByUserId(Long userId);

    @Query("SELECT d FROM Doctor d WHERE LOWER(d.specialisation) LIKE LOWER(CONCAT('%', :spec, '%'))")
    List<Doctor> findBySpecialisationContainingIgnoreCase(@Param("spec") String specialisation);

    boolean existsByUserId(Long userId);
}
