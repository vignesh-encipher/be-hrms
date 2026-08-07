package com.hrms.service;

import com.hrms.entity.Candidate;
import com.hrms.entity.InterviewFeedback;
import com.hrms.entity.Interview;
import com.hrms.entity.JobOpening;

import java.util.List;

public interface RecruitmentService {
    // Job Openings
    JobOpening createJobOpening(JobOpening jobOpening);
    JobOpening updateJobOpening(String id, JobOpening jobOpening);
    void deleteJobOpening(String id);
    JobOpening getJobOpening(String id);
    List<JobOpening> getAllJobOpenings();

    // Candidates
    Candidate addCandidate(Candidate candidate);
    List<Candidate> getAllCandidates();
    List<Candidate> getCandidates(String jobId, String stage);
    Candidate getCandidate(String id);
    Candidate moveToNextStage(String id);
    Candidate updateStage(String id, String stage);
    Candidate rejectCandidate(String id, String reason);
    Candidate addFeedback(String candidateId, InterviewFeedback feedback);

    // Interviews
    Interview scheduleInterview(Interview interview);
    List<Interview> getAllInterviews();
    List<Interview> getInterviewsForCandidate(String candidateId);
    Interview updateInterviewStatus(String id, String status);
}
