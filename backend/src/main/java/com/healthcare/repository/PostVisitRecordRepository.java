package com.healthcare.repository;

import com.healthcare.entity.PostVisitRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostVisitRecordRepository extends JpaRepository<PostVisitRecord, Long> {
    Optional<PostVisitRecord> findByAppointmentId(Long appointmentId);
}
