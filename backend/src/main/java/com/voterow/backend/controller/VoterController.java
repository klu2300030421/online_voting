package com.voterow.backend.controller;

import com.voterow.backend.dto.VoteRequest;
import com.voterow.backend.model.*;
import com.voterow.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/voter")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VOTER')")
public class VoterController {

    private final VoteRepository voteRepository;
    private final UserRepository userRepository;
    private final ElectionRepository electionRepository;
    private final CandidateRepository candidateRepository;

    @PostMapping("/elections/{electionId}/vote")
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