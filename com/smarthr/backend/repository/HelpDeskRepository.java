package com.smarthr.backend.repository;

import com.smarthr.backend.model.HelpDeskTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HelpDeskRepository extends JpaRepository<HelpDeskTicket, Long> {
    List<HelpDeskTicket> findAllByOrderByCreatedAtDesc();
    List<HelpDeskTicket> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
}