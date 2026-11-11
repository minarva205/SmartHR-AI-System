package com.smarthr.backend.repository;

import com.smarthr.backend.model.InterviewSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InterviewSlotRepository extends JpaRepository<InterviewSlot, Long> {
    List<InterviewSlot> findByRequestedByIdOrderByCreatedAtDesc(Long requestedById);
    List<InterviewSlot> findAllByOrderByCreatedAtDesc();
    long countByStatus(String status);
}
