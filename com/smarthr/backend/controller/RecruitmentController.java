package com.smarthr.backend.controller;

import com.smarthr.backend.model.*;
import com.smarthr.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/recruitment")
@CrossOrigin(origins = "*")
public class RecruitmentController {

    @Autowired
    private JobPostingRepository jobRepo;
    @Autowired
    private ApplicantRepository applicantRepo;

    @GetMapping("/jobs")
    public List<JobPosting> getJobs() {
        return jobRepo.findAll();
    }

    @PostMapping("/post-job")
    public JobPosting postJob(@RequestBody JobPosting job) {
        return jobRepo.save(job);
    }

    @GetMapping("/applicants")
    public List<Applicant> getApplicants() {
        return applicantRepo.findAll();
    }

    @PostMapping("/apply")
    public Applicant apply(@RequestBody Applicant applicant) {
        return applicantRepo.save(applicant);
    }

    @PutMapping("/applicant/{id}/status")
    public Applicant updateApplicantStatus(@PathVariable Long id, @RequestParam String status) {
        Applicant applicant = applicantRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Applicant not found"));
        applicant.setApplicationStatus(status);
        return applicantRepo.save(applicant);
    }

    @DeleteMapping("/applicant/{id}")
    public void deleteApplicant(@PathVariable Long id) {
        applicantRepo.deleteById(id);
    }

    @DeleteMapping("/job/{id}")
    public void deleteJob(@PathVariable Long id) {
        List<Applicant> apps = applicantRepo.findAll();
        for (Applicant app : apps) {
            if (app.getJobPosting() != null && app.getJobPosting().getId().equals(id)) {
                applicantRepo.delete(app);
            }
        }
        jobRepo.deleteById(id);
    }
}
