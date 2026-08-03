package com.voterow.backend.controller;

import com.voterow.backend.model.Vote;
import com.voterow.backend.model.Election;
import com.voterow.backend.model.User;
import com.voterow.backend.model.Candidate;
import com.voterow.backend.model.ElectionParticipant;
import com.voterow.backend.repository.VoteRepository;
import com.voterow.backend.repository.ElectionRepository;
import com.voterow.backend.repository.UserRepository;
import com.voterow.backend.repository.CandidateRepository;
import com.voterow.backend.repository.ElectionParticipantRepository;
import com.voterow.backend.service.ElectionWorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.security.Principal;
import java.util.List;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/voting")
public class VotingController {
    
    @Autowired
    private VoteRepository voteRepository;
    
    @Autowired
    private ElectionRepository electionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CandidateRepository candidateRepository;
    
    @Autowired
    private ElectionParticipantRepository electionParticipantRepository;
    
    @Autowired
    private ElectionWorkflowService electionWorkflowService;
    
    @PostMapping("/cast")
    public ResponseEntity<Map<String, Object>> castVote(@RequestBody Map<String, Object> voteData, Principal principal) {
        try {
            // Input validation
            if (voteData.get("voterId") == null || voteData.get("candidateId") == null || voteData.get("electionId") == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields"));
            }
            
            Long voterId = Long.valueOf(voteData.get("voterId").toString());
            Long candidateId = Long.valueOf(voteData.get("candidateId").toString());
            Long electionId = Long.valueOf(voteData.get("electionId").toString());
            
            // Validate IDs are positive
            if (voterId <= 0 || candidateId <= 0 || electionId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid ID values"));
            }
            
            // Check if election exists and is active
            Optional<Election> electionOpt = electionRepository.findById(electionId);
            if (!electionOpt.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Election not found"));
            }
            
            Election election = electionOpt.get();
            if (election.getStatus() != Election.ElectionStatus.ACTIVE) {
                return ResponseEntity.badRequest().body(Map.of("error", "Election is not active"));
            }
            
            // Check if election has assigned candidates or approved candidates exist
            List<ElectionParticipant> participants = electionParticipantRepository.findByElectionId(electionId);
            if (participants.isEmpty()) {
                // Auto-assign approved candidates to this election
                List<Candidate> approvedCandidates = candidateRepository.findByElectionIdAndStatus(electionId, Candidate.CandidateStatus.APPROVED);
                if (approvedCandidates.isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of("error", "No approved candidates for this election"));
                }
                // Auto-create participants
                for (Candidate candidate : approvedCandidates) {
                    ElectionParticipant participant = new ElectionParticipant();
                    participant.setElection(election);
                    participant.setCandidate(candidate);
                    participant.setUser(candidate.getUser());
                    participant.setIsActive(true);
                    participant.setAssignedAt(LocalDateTime.now());
                    electionParticipantRepository.save(participant);
                }
            }
            
            // Get entities
            if (principal == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }
            User voter = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Voter not found"));
            if (!voter.getId().equals(voterId)) {
                return ResponseEntity.status(403).body(Map.of("error", "You may only cast your own vote"));
            }
            Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new RuntimeException("Candidate not found"));
            
            // Use workflow service for secure vote casting
            Vote vote = electionWorkflowService.castVoteSecurely(voter, candidate, election);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Vote cast successfully");
            response.put("voteId", vote.getId());
            response.put("timestamp", vote.getVotedAt());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to cast vote: " + e.getMessage()));
        }
    }
    
    @GetMapping("/check/{voterId}/{electionId}")
    public ResponseEntity<Map<String, Object>> checkVoteStatus(@PathVariable Long voterId, @PathVariable Long electionId, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }
        Optional<User> voter = userRepository.findByEmail(principal.getName());
        if (voter.isPresent() && !voter.get().getId().equals(voterId)) {
            return ResponseEntity.status(403).body(Map.of("error", "You may only view your own voting status"));
        }
        Optional<Election> election = electionRepository.findById(electionId);
        
        if (voter.isEmpty() || election.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Optional<Vote> vote = voteRepository.findByVoterAndElection(voter.get(), election.get());
        
        Map<String, Object> response = new HashMap<>();
        response.put("hasVoted", vote.isPresent());
        if (vote.isPresent()) {
            response.put("voteTime", vote.get().getVotedAt());
        }
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/assign-candidates/{electionId}")
    public ResponseEntity<Map<String, Object>> assignCandidatesToElection(@PathVariable Long electionId) {
        try {
            Optional<Election> electionOpt = electionRepository.findById(electionId);
            if (!electionOpt.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Election not found"));
            }
            
            Election election = electionOpt.get();
            List<Candidate> approvedCandidates = candidateRepository.findByElectionIdAndStatus(electionId, Candidate.CandidateStatus.APPROVED);
            
            int assignedCount = 0;
            for (Candidate candidate : approvedCandidates) {
                if (!electionParticipantRepository.existsByElectionAndCandidate(election, candidate)) {
                    ElectionParticipant participant = new ElectionParticipant();
                    participant.setElection(election);
                    participant.setCandidate(candidate);
                    participant.setUser(candidate.getUser());
                    participant.setIsActive(true);
                    participant.setAssignedAt(LocalDateTime.now());
                    electionParticipantRepository.save(participant);
                    assignedCount++;
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", assignedCount + " candidates assigned to election",
                "assignedCount", assignedCount
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to assign candidates: " + e.getMessage()));
        }
    }
    
    private String generateVoteHash(Long voterId, Long candidateId, Long electionId) {
        try {
            SecureRandom secureRandom = new SecureRandom();
            byte[] salt = new byte[16];
            secureRandom.nextBytes(salt);
            
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String input = voterId + ":" + candidateId + ":" + electionId + ":" + 
                          System.currentTimeMillis() + ":" + java.util.UUID.randomUUID() + ":" + 
                          java.util.Base64.getEncoder().encodeToString(salt);
            
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate secure vote hash", e);
        }
    }
}
