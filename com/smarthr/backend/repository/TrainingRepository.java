package com.smarthr.backend.repository;

import com.smarthr.backend.model.TrainingProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainingRepository extends JpaRepository<TrainingProgress, Long> {
    List<TrainingProgress> findByEmployeeId(Long employeeId);
}