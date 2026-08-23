package com.healthcare.repository;

import com.healthcare.entity.DoctorLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorLeaveRepository extends JpaRepository<DoctorLeave, Long> {

    List<DoctorLeave> findByDoctorId(Long doctorId);

    Optional<DoctorLeave> findByDoctorIdAndLeaveDate(Long doctorId, LocalDate leaveDate);

    boolean existsByDoctorIdAndLeaveDate(Long doctorId, LocalDate leaveDate);

    @Query("SELECT dl FROM DoctorLeave dl WHERE dl.doctor.id = :doctorId " +
           "AND dl.leaveDate >= :from AND dl.leaveDate <= :to")
    List<DoctorLeave> findByDoctorIdAndDateRange(@Param("doctorId") Long doctorId,
                                                  @Param("from") LocalDate from,
                                                  @Param("to") LocalDate to);

    @Query("SELECT dl FROM DoctorLeave dl WHERE dl.patientsNotified = false")
    List<DoctorLeave> findUnnotifiedLeaves();
}
