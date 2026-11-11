package com.smarthr.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.smarthr.backend.model.SmartAlert;

import java.util.List;

@Repository
public interface SmartAlertRepository extends JpaRepository<SmartAlert, Long> {
    List<SmartAlert> findByEmployeeId(Long employeeId);
    List<SmartAlert> findByEmployeeIdAndResolved(Long employeeId, boolean resolved);
    List<SmartAlert> findByResolved(boolean resolved);
}
