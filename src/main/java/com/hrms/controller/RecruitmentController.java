package com.hrms.controller;

import com.hrms.entity.Candidate;
import com.hrms.entity.Interview;
import com.hrms.entity.InterviewFeedback;
import com.hrms.entity.JobOpening;
import com.hrms.service.RecruitmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/recruitment")
public class RecruitmentController {

    @Autowired
    private RecruitmentService recruitmentService;

    private static final String ROLES = "hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('RECRUITER')";

    // ---------- Job Openings ----------

    @PostMapping("/jobs")
    @PreAuthorize(ROLES)
    public ResponseEntity<JobOpening> createJobOpening(@RequestBody JobOpening jobOpening) {
        return ResponseEntity.ok(recruitmentService.createJobOpening(jobOpening));
    }

    @PutMapping("/jobs/{id}")
    @PreAuthorize(ROLES)
    public ResponseEntity<JobOpening> updateJobOpening(@PathVariable String id, @RequestBody JobOpening jobOpening) {
        return ResponseEntity.ok(recruitmentService.updateJobOpening(id, jobOpening));
    }

    @DeleteMapping("/jobs/{id}")
    @PreAuthorize(ROLES)
    public ResponseEntity<Void> deleteJobOpening(@PathVariable String id) {
        recruitmentService.deleteJobOpening(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/jobs/{id}")
    @PreAuthorize(ROLES)
    public ResponseEntity<JobOpening> getJobOpening(@PathVariable String id) {
        return ResponseEntity.ok(recruitmentService.getJobOpening(id));
    }

    @GetMapping("/jobs")
    @PreAuthorize(ROLES)
    public ResponseEntity<List<JobOpening>> getAllJobOpenings() {
        return ResponseEntity.ok(recruitmentService.getAllJobOpenings());
    }

    // ---------- Candidates ----------

    @PostMapping("/candidates")
    @PreAuthorize(ROLES)
    public ResponseEntity<Candidate> addCandidate(@RequestBody Candidate candidate) {
        return ResponseEntity.ok(recruitmentService.addCandidate(candidate));
    }

    @GetMapping("/candidates")
    @PreAuthorize(ROLES)
    public ResponseEntity<List<Candidate>> getCandidates(
            @RequestParam(required = false) String jobId,
            @RequestParam(required = false) String stage) {
        return ResponseEntity.ok(recruitmentService.getCandidates(jobId, stage));
    }

    @GetMapping("/candidates/{id}")
    @PreAuthorize(ROLES)
    public ResponseEntity<Candidate> getCandidate(@PathVariable String id) {
        return ResponseEntity.ok(recruitmentService.getCandidate(id));
    }

    @PostMapping("/candidates/{id}/next-stage")
    @PreAuthorize(ROLES)
    public ResponseEntity<Candidate> moveToNextStage(@PathVariable String id) {
        return ResponseEntity.ok(recruitmentService.moveToNextStage(id));
    }

    @PostMapping("/candidates/{id}/stage")
    @PreAuthorize(ROLES)
    public ResponseEntity<Candidate> updateStage(@PathVariable String id, @RequestParam String stage) {
        return ResponseEntity.ok(recruitmentService.updateStage(id, stage));
    }

    @PostMapping("/candidates/{id}/reject")
    @PreAuthorize(ROLES)
    public ResponseEntity<Candidate> rejectCandidate(@PathVariable String id, @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(recruitmentService.rejectCandidate(id, reason));
    }

    @PostMapping("/candidates/{id}/feedback")
    @PreAuthorize(ROLES)
    public ResponseEntity<Candidate> addFeedback(@PathVariable String id, @RequestBody InterviewFeedback feedback) {
        return ResponseEntity.ok(recruitmentService.addFeedback(id, feedback));
    }

    // ---------- Interviews ----------

    @PostMapping("/interviews")
    @PreAuthorize(ROLES)
    public ResponseEntity<Interview> scheduleInterview(@RequestBody Interview interview) {
        return ResponseEntity.ok(recruitmentService.scheduleInterview(interview));
    }

    @GetMapping("/interviews")
    @PreAuthorize(ROLES)
    public ResponseEntity<List<Interview>> getAllInterviews() {
        return ResponseEntity.ok(recruitmentService.getAllInterviews());
    }

    @GetMapping("/interviews/candidate/{candidateId}")
    @PreAuthorize(ROLES)
    public ResponseEntity<List<Interview>> getInterviewsForCandidate(@PathVariable String candidateId) {
        return ResponseEntity.ok(recruitmentService.getInterviewsForCandidate(candidateId));
    }

    @PostMapping("/interviews/{id}/status")
    @PreAuthorize(ROLES)
    public ResponseEntity<Interview> updateInterviewStatus(@PathVariable String id, @RequestParam String status) {
        return ResponseEntity.ok(recruitmentService.updateInterviewStatus(id, status));
    }
}
