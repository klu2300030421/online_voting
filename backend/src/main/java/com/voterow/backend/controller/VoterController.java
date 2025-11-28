package com.voterow.backend.controller;

import com.voterow.backend.dto.VoteRequest;
import com.voterow.backend.model.*;
import com.voterow.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/voter")
@RequiredArgsConstructor
public class VoterController {

    private static final Logger logger = LoggerFactory.getLogger(VoterController.class);

    private final VoteRepository voteRepository;
    private final UserRepository userRepository;
    private final ElectionRepository electionRepository;
    private final CandidateRepository candidateRepository;

    /**
     * Get all active elections available for voting (public endpoint, no auth required for listing)
     */
    @GetMapping("/elections")
    public ResponseEntity<List<Map<String, Object>>> getAvailableElections() {
        try {
            logger.debug("Fetching ALL elections from database");
            List<Election> elections = electionRepository.findAll();
            logger.debug("Found {} elections in database", elections.size());
            List<Map<String, Object>> electionsList = new ArrayList<>();
            
            for (Election election : elections) {
                Map<String, Object> electionMap = new HashMap<>();
                electionMap.put("id", election.getId());
                electionMap.put("title", election.getTitle());
                electionMap.put("description", election.getDescription());
                electionMap.put("startDate", election.getStartDate() != null ? election.getStartDate().toString() : null);
                electionMap.put("endDate", election.getEndDate() != null ? election.getEndDate().toString() : null);
                electionMap.put("status", election.getStatus() != null ? election.getStatus().toString() : "DRAFT");
                electionMap.put("createdAt", election.getCreatedAt() != null ? election.getCreatedAt().toString() : null);
                electionMap.put("isActive", election.getIsActive());
                electionsList.add(electionMap);
            }
            
            logger.debug("Returning {} available elections", electionsList.size());
            return ResponseEntity.ok(electionsList);
        } catch (Exception e) {
            System.err.println("Error fetching elections: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    @PostMapping("/elections/{electionId}/vote")
    @PreAuthorize("hasRole('VOTER')")
    public ResponseEntity<String> castVote(@PathVariable Long electionId, @RequestBody VoteRequest voteRequest, Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName()).orElseThrow(() -> new RuntimeException("User not found"));
        Election election = electionRepository.findById(electionId).orElseThrow(() -> new RuntimeException("Election not found"));
        Candidate candidate = candidateRepository.findById(voteRequest.getParticipantId()).orElseThrow(() -> new RuntimeException("Candidate not found"));

        if (voteRepository.existsByElectionAndVoter(election, currentUser)) {
            return ResponseEntity.badRequest().body("You have already voted in this election.");
        }

        Vote vote = new Vote();
        vote.setElection(election);
        vote.setVoter(currentUser);
        vote.setCandidate(candidate);
        vote.setVotedAt(LocalDateTime.now());
        vote.setEncryptedVote("ENCRYPTED_VOTE_DATA"); // Placeholder for actual encryption

        voteRepository.save(vote);

        return ResponseEntity.ok("Vote cast successfully.");
    }
}