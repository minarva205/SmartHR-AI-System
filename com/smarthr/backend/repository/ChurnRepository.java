package com.smarthr.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.smarthr.backend.model.ChurnRecord;

import java.util.Optional;

@Repository
public interface ChurnRepository extends JpaRepository<ChurnRecord, Long> {
    Optional<ChurnRecord> findByEmployeeId(Long employeeId);
}
