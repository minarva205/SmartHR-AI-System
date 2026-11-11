package com.smarthr.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.smarthr.backend.model.ExpenseClaim;

import java.util.List;

@Repository
public interface ExpenseClaimRepository extends JpaRepository<ExpenseClaim, Long> {
    List<ExpenseClaim> findByEmployeeId(Long employeeId);
    List<ExpenseClaim> findByStatus(String status);
    List<ExpenseClaim> findByEmployeeIdAndStatus(Long employeeId, String status);
}
