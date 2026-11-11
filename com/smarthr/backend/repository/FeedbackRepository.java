package com.smarthr.backend.repository;

import com.smarthr.backend.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findByEmployeeIdOrderBySubmittedAtDesc(Long employeeId);

    List<Feedback> findAllByOrderBySubmittedAtDesc();
}