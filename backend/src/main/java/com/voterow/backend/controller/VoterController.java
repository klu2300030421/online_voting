package com.voterow.backend.controller;

import com.voterow.backend.dto.VoteRequest;
import com.voterow.backend.model.*;
import com.voterow.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/voter")
@RequiredArgsConstructor
public class VoterController {

    private final VoteRepository voteRepository;
    private final UserRepository userRepository;
    private final ElectionRepository electionRepository;
    private final CandidateRepository candidateRepository;
    private final ElectionParticipantRepository electionParticipantRepository;

    /**
     * Get all elections with participants built from ElectionParticipant table
     * (most reliable source after admin assigns candidates)
     */
    @GetMapping("/elections")
    @Transactional
    public ResponseEntity<List<Map<String, Object>>> getAvailableElections() {
        try {
            List<Election> elections = electionRepository.findAll();
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

                // Build participants from ElectionParticipant table (primary source)
                Set<Long> participantCandidateIds = new LinkedHashSet<>();
                List<ElectionParticipant> eps = electionParticipantRepository.findByElectionAndIsActiveTrue(election);
                for (ElectionParticipant ep : eps) {
                    if (ep.getCandidate() != null) {
                        participantCandidateIds.add(ep.getCandidate().getId());
                    }
                }
                // Also include candidates linked via candidate.election_id (fallback)
                List<Candidate> linked = candidateRepository.findByElectionIdAndStatus(
                        election.getId(), Candidate.CandidateStatus.APPROVED);
                for (Candidate c : linked) {
                    participantCandidateIds.add(c.getId());
                }

                electionMap.put("participants", new ArrayList<>(participantCandidateIds));
                electionMap.put("candidateCount", participantCandidateIds.size());
                electionsList.add(electionMap);
            }

            return ResponseEntity.ok(electionsList);
        } catch (Exception e) {
            System.err.println("Error fetching elections: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    /**
     * Get all approved candidates for voter dashboard
     * Includes candidates from both Candidate table and ElectionParticipant table
     */
    @GetMapping("/candidates")
    @Transactional
    public ResponseEntity<List<Map<String, Object>>> getApprovedCandidates() {
        try {
            List<Candidate> approvedCandidates = candidateRepository.findByStatus(Candidate.CandidateStatus.APPROVED);
            List<Map<String, Object>> result = new ArrayList<>();
            Set<Long> addedIds = new HashSet<>();

            for (Candidate candidate : approvedCandidates) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", candidate.getId());
                row.put("userId", candidate.getUser() != null ? candidate.getUser().getId() : null);
                row.put("name", candidate.getUser() != null ? candidate.getUser().getFullName() : "");
                row.put("email", candidate.getUser() != null ? candidate.getUser().getEmail() : "");
                row.put("party", candidate.getPartyName() != null ? candidate.getPartyName() : "Independent");
                row.put("electionId", candidate.getElection() != null ? candidate.getElection().getId() : null);
                row.put("status", "APPROVED");
                result.add(row);
                addedIds.add(candidate.getId());
            }

            // Also include candidates assigned via ElectionParticipant but not yet in Candidate table
            List<ElectionParticipant> allEps = electionParticipantRepository.findAll();
            for (ElectionParticipant ep : allEps) {
                if (ep.getCandidate() != null && !addedIds.contains(ep.getCandidate().getId())) {
                    Candidate c = ep.getCandidate();
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", c.getId());
                    row.put("userId", c.getUser() != null ? c.getUser().getId() : null);
                    row.put("name", c.getUser() != null ? c.getUser().getFullName() : "");
                    row.put("email", c.getUser() != null ? c.getUser().getEmail() : "");
                    row.put("party", c.getPartyName() != null ? c.getPartyName() : "Independent");
                    row.put("electionId", ep.getElection() != null ? ep.getElection().getId() : null);
                    row.put("status", "APPROVED");
                    result.add(row);
                    addedIds.add(c.getId());
                }
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("Error fetching approved candidates for voters: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    /**
     * Get published election results (only RESULTS_PUBLISHED elections)
     */
    @GetMapping("/results")
    @Transactional
    public ResponseEntity<List<Map<String, Object>>> getPublishedResults() {
        try {
            List<Election> published = electionRepository.findAll().stream()
                    .filter(e -> e.getStatus() == Election.ElectionStatus.RESULTS_PUBLISHED
                              || e.getStatus() == Election.ElectionStatus.COMPLETED)
                    .toList();

            List<Map<String, Object>> resultList = new ArrayList<>();

            for (Election election : published) {
                List<Vote> votes = voteRepository.findByElection(election);

                // Tally votes per candidate
                Map<Long, Long> tally = new LinkedHashMap<>();
                for (Vote v : votes) {
                    if (v.getCandidate() != null) {
                        tally.merge(v.getCandidate().getId(), 1L, Long::sum);
                    }
                }

                // Build candidate result rows
                List<Map<String, Object>> candidateResults = new ArrayList<>();
                long maxVotes = 0;
                Long winnerId = null;

                // Collect all candidates for this election
                Set<Long> candidateIds = new LinkedHashSet<>(tally.keySet());
                List<ElectionParticipant> eps = electionParticipantRepository.findByElectionAndIsActiveTrue(election);
                for (ElectionParticipant ep : eps) {
                    if (ep.getCandidate() != null) candidateIds.add(ep.getCandidate().getId());
                }
                List<Candidate> linked = candidateRepository.findByElectionIdAndStatus(
                        election.getId(), Candidate.CandidateStatus.APPROVED);
                for (Candidate c : linked) candidateIds.add(c.getId());

                for (Long cid : candidateIds) {
                    Candidate c = candidateRepository.findById(cid).orElse(null);
                    if (c == null) continue;
                    long voteCount = tally.getOrDefault(cid, 0L);
                    double pct = votes.isEmpty() ? 0.0 : (voteCount * 100.0) / votes.size();
                    Map<String, Object> cr = new HashMap<>();
                    cr.put("candidateId", cid);
                    cr.put("candidateName", c.getUser() != null ? c.getUser().getFullName() : "Unknown");
                    cr.put("party", c.getPartyName() != null ? c.getPartyName() : "Independent");
                    cr.put("votes", voteCount);
                    cr.put("percentage", Math.round(pct * 10.0) / 10.0);
                    candidateResults.add(cr);
                    if (voteCount > maxVotes) { maxVotes = voteCount; winnerId = cid; }
                }

                // Sort by votes descending and assign rank
                candidateResults.sort((a, b) -> Long.compare((Long) b.get("votes"), (Long) a.get("votes")));
                for (int i = 0; i < candidateResults.size(); i++) {
                    candidateResults.get(i).put("rank", i + 1);
                    candidateResults.get(i).put("isWinner", candidateResults.get(i).get("candidateId").equals(winnerId));
                }

                Map<String, Object> electionResult = new HashMap<>();
                electionResult.put("electionId", election.getId());
                electionResult.put("electionTitle", election.getTitle());
                electionResult.put("status", election.getStatus().toString());
                electionResult.put("totalVotes", votes.size());
                electionResult.put("winnerId", winnerId);
                electionResult.put("candidateResults", candidateResults);
                resultList.add(electionResult);
            }

            return ResponseEntity.ok(resultList);
        } catch (Exception e) {
            System.err.println("Error fetching published results: " + e.getMessage());
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
        vote.setEncryptedVote("ENCRYPTED_VOTE_DATA");

        voteRepository.save(vote);

        return ResponseEntity.ok("Vote cast successfully.");
    }
}
