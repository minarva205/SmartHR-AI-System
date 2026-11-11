package com.smarthr.backend.repository;

import com.smarthr.backend.model.ParsedResume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface ParsedResumeRepository extends JpaRepository<ParsedResume, Long> {
    Optional<ParsedResume> findByEmail(String email);
    List<ParsedResume> findByApplicantUserId(Long userId);
    Optional<ParsedResume> findTopByApplicantUserIdOrderByParsedAtDesc(Long userId);
}
