package com.voterow.backend.service;

import com.voterow.backend.model.Candidate;
import com.voterow.backend.model.Election;
import com.voterow.backend.model.User;
import com.voterow.backend.repository.CandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CandidateService {
    
    private final CandidateRepository candidateRepository;
    private final AuditLogService auditLogService;
    
    public List<Candidate> getAllCandidates() {
        return candidateRepository.findAll();
    }
    
    public List<Candidate> getCandidatesByElection(Election election) {
        return candidateRepository.findByElectionAndIsActiveTrue(election);
    }
    
    public List<Candidate> getCandidatesByStatus(Candidate.CandidateStatus status) {
        return candidateRepository.findByStatus(status);
    }
    
    public Optional<Candidate> getCandidateById(Long id) {
        return candidateRepository.findById(id);
    }
    
    public Candidate registerCandidate(Candidate candidate, User registeredBy) {
        candidate.setCreatedAt(LocalDateTime.now());
        candidate.setUpdatedAt(LocalDateTime.now());
        candidate.setStatus(Candidate.CandidateStatus.PENDING);
        
        Candidate saved = candidateRepository.save(candidate);
        
        auditLogService.logAction(registeredBy, "REGISTER_CANDIDATE", "Candidate", saved.getId(), 
            "Registered candidate: " + candidate.getUser().getFullName() + " for election: " + candidate.getElection().getTitle());
        
        return saved;
    }
    
    public Candidate updateCandidate(Long id, Candidate candidateDetails, User updatedBy) {
        Candidate candidate = candidateRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Candidate not found"));
        
        candidate.setPartyName(candidateDetails.getPartyName());
        candidate.setPartySymbol(candidateDetails.getPartySymbol());
        candidate.setManifesto(candidateDetails.getManifesto());
        candidate.setQualifications(candidateDetails.getQualifications());
        candidate.setUpdatedAt(LocalDateTime.now());
        
        Candidate saved = candidateRepository.save(candidate);
        
        auditLogService.logAction(updatedBy, "UPDATE_CANDIDATE", "Candidate", saved.getId(), 
            "Updated candidate: " + candidate.getUser().getFullName());
        
        return saved;
    }
    
    public Candidate approveCandidate(Long id, User approvedBy) {
        Candidate candidate = candidateRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Candidate not found"));
        
        if (candidate.getStatus() != Candidate.CandidateStatus.PENDING) {
            throw new RuntimeException("Only pending candidates can be approved");
        }
        
        candidate.setStatus(Candidate.CandidateStatus.APPROVED);
        candidate.setApprovedBy(approvedBy);
        candidate.setApprovedAt(LocalDateTime.now());
        candidate.setUpdatedAt(LocalDateTime.now());
        
        Candidate saved = candidateRepository.save(candidate);
        
        auditLogService.logAction(approvedBy, "APPROVE_CANDIDATE", "Candidate", saved.getId(), 
            "Approved candidate: " + candidate.getUser().getFullName());
        
        return saved;
    }
    
    public Candidate rejectCandidate(Long id, String reason, User rejectedBy) {
        Candidate candidate = candidateRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Candidate not found"));
        
        if (candidate.getStatus() != Candidate.CandidateStatus.PENDING) {
            throw new RuntimeException("Only pending candidates can be rejected");
        }
        
        candidate.setStatus(Candidate.CandidateStatus.REJECTED);
        candidate.setRejectionReason(reason);
        candidate.setUpdatedAt(LocalDateTime.now());
        
        Candidate saved = candidateRepository.save(candidate);
        
        auditLogService.logAction(rejectedBy, "REJECT_CANDIDATE", "Candidate", saved.getId(), 
            "Rejected candidate: " + candidate.getUser().getFullName() + " Reason: " + reason);
        
        return saved;
    }
    
    public void deleteCandidate(Long id, User deletedBy) {
        Candidate candidate = candidateRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Candidate not found"));
        
        candidate.setIsActive(false);
        candidate.setUpdatedAt(LocalDateTime.now());
        candidateRepository.save(candidate);
        
        auditLogService.logAction(deletedBy, "DELETE_CANDIDATE", "Candidate", id, 
            "Deleted candidate: " + candidate.getUser().getFullName());
    }
    
    public boolean isCandidateAlreadyRegistered(User user, Election election) {
        return candidateRepository.existsByUserAndElection(user, election);
    }
    
    public List<Candidate> getCandidatesByUser(User user) {
        return candidateRepository.findByUser(user);
    }
}