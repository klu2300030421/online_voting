package com.voterow.backend.service;

import com.voterow.backend.model.*;
import com.voterow.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ElectionWorkflowService {

    @Autowired
    private ElectionRepository electionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private VoteRepository voteRepository;
    
    @Autowired
    private ElectionParticipantRepository electionParticipantRepository;

    @Autowired
    private AuditLogService auditLogService;
    
    @Autowired
    private NotificationService notificationService;

    // Step 1: Create Election with Eligibility Rules
    public Election createElectionWithRules(Election election, ElectionEligibility eligibility, User admin) {
        election.setStatus(Election.ElectionStatus.DRAFT);
        election.setCreatedAt(LocalDateTime.now());
        Election savedElection = electionRepository.save(election);
        
        eligibility.setElection(savedElection);
        // Save eligibility rules (repository needed)
        
        auditLogService.logAction(admin, "CREATE_ELECTION", "Election", savedElection.getId(), 
            "Election created: " + savedElection.getTitle());
        
        return savedElection;
    }

    // Step 2: Approve Participants
    public void approveParticipant(Long candidateId, User admin) {
        // Implementation handled in AdminController
        auditLogService.logAction(admin, "APPROVE_PARTICIPANT", "Candidate", candidateId, 
            "Participant approved for election");
    }

    // Step 3: Open Election for Voting
    public Election openElection(Long electionId, User admin) {
        Election election = electionRepository.findById(electionId)
            .orElseThrow(() -> new RuntimeException("Election not found"));
        
        election.setStatus(Election.ElectionStatus.ACTIVE);
        election.setUpdatedAt(LocalDateTime.now());
        Election savedElection = electionRepository.save(election);
        
        // Generate voter access tokens
        generateVoterAccessTokens(savedElection);
        
        auditLogService.logAction(admin, "OPEN_ELECTION", "Election", electionId, 
            "Election opened for voting");
        
        return savedElection;
    }

    // Step 4: Validate Voter Eligibility
    public boolean validateVoterEligibility(User voter, Election election) {
        // Check if voter is active
        if (!Boolean.TRUE.equals(voter.getIsActive())) {
            return false;
        }
        
        // Check if already voted
        if (voteRepository.existsByElectionAndVoter(election, voter)) {
            return false;
        }
        
        // Check election is active
        return election.getStatus() == Election.ElectionStatus.ACTIVE;
    }

    // Step 5: Cast Vote with Validation and Audit
    public Vote castVoteSecurely(User voter, Candidate candidate, Election election) {
        // Validate eligibility
        if (!validateVoterEligibility(voter, election)) {
            throw new RuntimeException("Voter not eligible for this election");
        }
        // Check candidate is approved and active
        if (candidate.getStatus() != Candidate.CandidateStatus.APPROVED ||
                !Boolean.TRUE.equals(candidate.getIsActive())) {
            throw new RuntimeException("Candidate is not eligible for this election");
        }
        // Check candidate is assigned to this election (via ElectionParticipant OR candidate.election)
        boolean assignedViaParticipant = electionParticipantRepository
                .existsByElectionIdAndCandidateId(election.getId(), candidate.getId());
        boolean assignedViaElection = candidate.getElection() != null &&
                candidate.getElection().getId().equals(election.getId());
        if (!assignedViaParticipant && !assignedViaElection) {
            throw new RuntimeException("Candidate is not assigned to this election");
        }
        
        // Create and save vote atomically
        Vote vote = new Vote(voter, candidate, election);
        vote.setVoteHash(generateSecureVoteHash(voter.getId(), candidate.getId(), election.getId()));
        Vote savedVote = voteRepository.save(vote);
        
        // Audit log
        auditLogService.logAction(voter, "CAST_VOTE", "Vote", savedVote.getId(), 
            "Vote cast in election: " + election.getTitle());
        
        // Send confirmation notification
        notificationService.sendVoteConfirmation(voter, election, savedVote.getId());
        
        return savedVote;
    }

    // Step 6: Close Election and Publish Results
    public Election closeElection(Long electionId, User admin) {
        Election election = electionRepository.findById(electionId)
            .orElseThrow(() -> new RuntimeException("Election not found"));
        
        election.setStatus(Election.ElectionStatus.COMPLETED);
        election.setUpdatedAt(LocalDateTime.now());
        Election savedElection = electionRepository.save(election);
        
        // Publish results
        publishElectionResults(savedElection);
        
        auditLogService.logAction(admin, "CLOSE_ELECTION", "Election", electionId, 
            "Election closed and results published");
        
        return savedElection;
    }

    private void generateVoterAccessTokens(Election election) {
        List<User> eligibleVoters = userRepository.findByUserTypeAndIsActiveAndIsVerified(
            UserType.ROLE_VOTER, true, true);
        
        for (User voter : eligibleVoters) {
            if (validateVoterEligibility(voter, election)) {
                String accessToken = UUID.randomUUID().toString();
                // Save voter access token (repository needed)
                notificationService.sendVoterAccessNotification(voter, election, accessToken);
            }
        }
    }

    private void publishElectionResults(Election election) {
        // Calculate and publish results
        List<Object[]> results = voteRepository.countVotesByCandidate(election.getId());
        // Implementation for result publishing
        
        // Notify all participants
        notificationService.sendResultsNotification(election, results);
    }

    // Step 7: Publish Election (transition from DRAFT to PUBLISHED)
    public Election publishElection(Long electionId, User admin) {
        Election election = electionRepository.findById(electionId)
            .orElseThrow(() -> new RuntimeException("Election not found"));
        
        // Election must be in DRAFT state to be published
        if (election.getStatus() != Election.ElectionStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT elections can be published. Current status: " + election.getStatus());
        }
        
        // Validate election has required fields
        if (election.getTitle() == null || election.getTitle().isEmpty()) {
            throw new RuntimeException("Election title is required");
        }
        if (election.getStartDate() == null || election.getEndDate() == null) {
            throw new RuntimeException("Election start and end dates are required");
        }
        if (election.getEndDate().isBefore(election.getStartDate())) {
            throw new RuntimeException("Election end date must be after start date");
        }
        
        election.setStatus(Election.ElectionStatus.SCHEDULED);
        election.setUpdatedAt(LocalDateTime.now());
        Election savedElection = electionRepository.save(election);
        
        auditLogService.logAction(admin, "PUBLISH_ELECTION", "Election", electionId, 
            "Election published: " + savedElection.getTitle());
        
        return savedElection;
    }

    // Step 7b: Get eligible voters for election
    public List<User> getEligibleVoters(Long electionId) {
        Election election = electionRepository.findById(electionId)
            .orElseThrow(() -> new RuntimeException("Election not found"));
        
        return userRepository.findByUserTypeAndIsActiveAndIsVerified(
            UserType.ROLE_VOTER, true, true)
            .stream()
            .filter(voter -> validateVoterEligibility(voter, election))
            .toList();
    }

    // Step 8: Tally votes and prepare results
    public Map<String, Object> calculateElectionResults(Long electionId) {
        Election election = electionRepository.findById(electionId)
            .orElseThrow(() -> new RuntimeException("Election not found"));
        
        List<Vote> allVotes = voteRepository.findByElection(election);
        
        Map<String, Object> results = new java.util.HashMap<>();
        results.put("electionId", electionId);
        results.put("electionTitle", election.getTitle());
        results.put("totalVotes", allVotes.size());
        results.put("calculatedAt", LocalDateTime.now());
        
        // Group votes by candidate
        java.util.Map<Long, Long> votesByCandidate = allVotes.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                vote -> vote.getCandidate().getId(),
                java.util.stream.Collectors.counting()
            ));
        
        // Create results per candidate
        List<Map<String, Object>> candidateResults = new java.util.ArrayList<>();
        long maxVotes = 0;
        Long winnerId = null;
        
        for (java.util.Map.Entry<Long, Long> entry : votesByCandidate.entrySet()) {
            Map<String, Object> candidateResult = new java.util.HashMap<>();
            candidateResult.put("candidateId", entry.getKey());
            candidateResult.put("votes", entry.getValue());
            
            if (allVotes.size() > 0) {
                candidateResult.put("percentage", (entry.getValue() * 100.0) / allVotes.size());
            } else {
                candidateResult.put("percentage", 0.0);
            }
            
            candidateResults.add(candidateResult);
            
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                winnerId = entry.getKey();
            }
        }
        
        results.put("candidateResults", candidateResults);
        results.put("winnerId", winnerId);
        results.put("winnerVotes", maxVotes);
        
        return results;
    }

    private String generateSecureVoteHash(Long voterId, Long candidateId, Long electionId) {
        try {
            java.security.SecureRandom secureRandom = new java.security.SecureRandom();
            byte[] salt = new byte[16];
            secureRandom.nextBytes(salt);
            
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            String input = voterId + ":" + candidateId + ":" + electionId + ":" + 
                          System.currentTimeMillis() + ":" + UUID.randomUUID() + ":" + 
                          java.util.Base64.getEncoder().encodeToString(salt);
            
            byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate secure vote hash", e);
        }
    }
}
