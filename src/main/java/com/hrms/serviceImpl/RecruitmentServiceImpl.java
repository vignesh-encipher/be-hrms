package com.hrms.serviceImpl;

import com.hrms.entity.Candidate;
import com.hrms.entity.Interview;
import com.hrms.entity.InterviewFeedback;
import com.hrms.entity.JobOpening;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.CandidateRepository;
import com.hrms.repository.InterviewRepository;
import com.hrms.repository.JobOpeningRepository;
import com.hrms.service.RecruitmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RecruitmentServiceImpl implements RecruitmentService {

    @Autowired
    private JobOpeningRepository jobOpeningRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private InterviewRepository interviewRepository;

    private static final List<String> STAGE_ORDER = List.of("Applied", "Screening", "Interview", "Offer", "Joined");

    @Override
    public JobOpening createJobOpening(JobOpening jobOpening) {
        if (jobOpening.getStatus() == null || jobOpening.getStatus().isEmpty()) {
            jobOpening.setStatus("Open");
        }
        return jobOpeningRepository.save(jobOpening);
    }

    @Override
    public JobOpening updateJobOpening(String id, JobOpening jobOpening) {
        JobOpening existing = jobOpeningRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job opening not found with id: " + id));
        existing.setTitle(jobOpening.getTitle());
        existing.setDepartment(jobOpening.getDepartment());
        existing.setDesignation(jobOpening.getDesignation());
        existing.setPositions(jobOpening.getPositions());
        existing.setStatus(jobOpening.getStatus());
        existing.setRequisitionId(jobOpening.getRequisitionId());
        existing.setDescription(jobOpening.getDescription());
        existing.setLocation(jobOpening.getLocation());
        return jobOpeningRepository.save(existing);
    }

    @Override
    public void deleteJobOpening(String id) {
        if (!jobOpeningRepository.existsById(id)) {
            throw new ResourceNotFoundException("Job opening not found with id: " + id);
        }
        jobOpeningRepository.deleteById(id);
    }

    @Override
    public JobOpening getJobOpening(String id) {
        return jobOpeningRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job opening not found with id: " + id));
    }

    @Override
    public List<JobOpening> getAllJobOpenings() {
        return jobOpeningRepository.findAll();
    }

    @Override
    public Candidate addCandidate(Candidate candidate) {
        if (candidate.getStage() == null || candidate.getStage().isEmpty()) {
            candidate.setStage("Applied");
        }
        if (candidate.getFeedbackList() == null) {
            candidate.setFeedbackList(new ArrayList<>());
        }
        return candidateRepository.save(candidate);
    }

    @Override
    public List<Candidate> getAllCandidates() {
        return candidateRepository.findAll();
    }

    @Override
    public List<Candidate> getCandidates(String jobId, String stage) {
        if (jobId != null && !jobId.isEmpty() && stage != null && !stage.isEmpty()) {
            return candidateRepository.findByJobIdAndStage(jobId, stage);
        } else if (jobId != null && !jobId.isEmpty()) {
            return candidateRepository.findByJobId(jobId);
        } else if (stage != null && !stage.isEmpty()) {
            return candidateRepository.findByStage(stage);
        }
        return candidateRepository.findAll();
    }

    @Override
    public Candidate getCandidate(String id) {
        return candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found with id: " + id));
    }

    @Override
    public Candidate moveToNextStage(String id) {
        Candidate candidate = getCandidate(id);
        String currentStage = candidate.getStage();
        int idx = STAGE_ORDER.indexOf(currentStage);
        if (idx < 0) {
            throw new BadRequestException("Cannot progress candidate from stage: " + currentStage);
        }
        if (idx >= STAGE_ORDER.size() - 1) {
            throw new BadRequestException("Candidate is already at the final stage: " + currentStage);
        }
        candidate.setStage(STAGE_ORDER.get(idx + 1));
        return candidateRepository.save(candidate);
    }

    @Override
    public Candidate updateStage(String id, String stage) {
        Candidate candidate = getCandidate(id);
        candidate.setStage(stage);
        return candidateRepository.save(candidate);
    }

    @Override
    public Candidate rejectCandidate(String id, String reason) {
        Candidate candidate = getCandidate(id);
        candidate.setStage("Rejected");
        candidate.setRejectionReason(reason);
        return candidateRepository.save(candidate);
    }

    @Override
    public Candidate addFeedback(String candidateId, InterviewFeedback feedback) {
        Candidate candidate = getCandidate(candidateId);
        if (candidate.getFeedbackList() == null) {
            candidate.setFeedbackList(new ArrayList<>());
        }
        if (feedback.getSubmittedAt() == null) {
            feedback.setSubmittedAt(LocalDateTime.now());
        }
        candidate.getFeedbackList().add(feedback);
        return candidateRepository.save(candidate);
    }

    @Override
    public Interview scheduleInterview(Interview interview) {
        if (interview.getStatus() == null || interview.getStatus().isEmpty()) {
            interview.setStatus("Scheduled");
        }
        return interviewRepository.save(interview);
    }

    @Override
    public List<Interview> getAllInterviews() {
        return interviewRepository.findAll();
    }

    @Override
    public List<Interview> getInterviewsForCandidate(String candidateId) {
        return interviewRepository.findByCandidateId(candidateId);
    }

    @Override
    public Interview updateInterviewStatus(String id, String status) {
        Interview interview = interviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found with id: " + id));
        interview.setStatus(status);
        return interviewRepository.save(interview);
    }
}
