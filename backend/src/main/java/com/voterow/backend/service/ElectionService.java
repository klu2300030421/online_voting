package com.voterow.backend.service;

import com.voterow.backend.dto.ElectionRequest;
import com.voterow.backend.model.Election;
import com.voterow.backend.model.User;
import com.voterow.backend.model.UserType;
import com.voterow.backend.repository.ElectionRepository;
import com.voterow.backend.repository.UserRepository;
import com.voterow.backend.repository.VoteRepository;
import com.voterow.backend.repository.CandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ElectionService {
    
    private final ElectionRepository electionRepository;
    private final UserRepository userRepository;
    private final VoteRepository voteRepository;
    private final CandidateRepository candidateRepository;
    
    // Keep existing methods for backward compatibility
    @Transactional
    public Election createElection(ElectionRequest request) {
        Election election = new Election();
        election.setTitle(request.getTitle());
        // Convert Instant to LocalDateTime for consistency
        election.setStartDate(LocalDateTime.ofInstant(request.getStartDate(), java.time.ZoneId.systemDefault()));
        election.setEndDate(LocalDateTime.ofInstant(request.getEndDate(), java.time.ZoneId.systemDefault()));
        election.setCreatedAt(LocalDateTime.now());
        election.setUpdatedAt(LocalDateTime.now());
        return electionRepository.save(election);
    }

    @Transactional
    public Election updateVoters(Long electionId, List<Long> voterIds) {
        Election election = electionRepository.findById(electionId).orElseThrow(() -> new RuntimeException("Election not found"));
        Set<User> voters = voterIds.stream()
                .map(id -> userRepository.findById(id).orElseThrow(() -> new RuntimeException("Voter with ID " + id + " not found")))
                .collect(Collectors.toSet());
        election.setVoters(voters);
        return electionRepository.save(election);
    }

    @Transactional
    public Election enrollParticipant(Long electionId, User participant) {
        Election election = electionRepository.findById(electionId).orElseThrow(() -> new RuntimeException("Election not found"));
        election.getParticipants().add(participant);
        return electionRepository.save(election);
    }
    
    // Enhanced methods for comprehensive admin dashboard
    public List<Election> getAllElections() {
        return electionRepository.findByIsActiveTrueOrderByCreatedAtDesc();
    }
    
    public List<Election> getElectionsByStatus(Election.ElectionStatus status) {
        return electionRepository.findByStatusOrderByCreatedAtDesc(status);
    }
    
    public List<Election> getCurrentlyActiveElections() {
        return electionRepository.findCurrentlyActiveElections(LocalDateTime.now());
    }
    
    public List<Election> getUpcomingElections() {
        return electionRepository.findUpcomingElections(LocalDateTime.now());
    }
    
    public List<Election> getCompletedElections() {
        return electionRepository.findCompletedElections(LocalDateTime.now());
    }
    
    public Optional<Election> getElectionById(Long id) {
        return electionRepository.findById(id);
    }
    
    public Election createElection(Election election, User createdBy) {
        election.setCreatedBy(createdBy);
        election.setCreatedAt(LocalDateTime.now());
        election.setUpdatedAt(LocalDateTime.now());
        return electionRepository.save(election);
    }
    
    public Election updateElection(Long id, Election electionDetails, User updatedBy) {
        Election election = electionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Election not found"));
        
        election.setTitle(electionDetails.getTitle());
        election.setDescription(electionDetails.getDescription());
        election.setStartDate(electionDetails.getStartDate());
        election.setEndDate(electionDetails.getEndDate());
        election.setStatus(electionDetails.getStatus());
        election.setType(electionDetails.getType());
        election.setAllowMultipleVotes(electionDetails.getAllowMultipleVotes());
        election.setMinAge(electionDetails.getMinAge());
        election.setEligibilityRules(electionDetails.getEligibilityRules());
        election.setUpdatedAt(LocalDateTime.now());
        
        return electionRepository.save(election);
    }
    
    public void deleteElection(Long id, User deletedBy) {
        Election election = electionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Election not found"));
        
        election.setIsActive(false);
        election.setUpdatedAt(LocalDateTime.now());
        electionRepository.save(election);
    }
    
    public Election startElection(Long id, User startedBy) {
        Election election = electionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Election not found"));
        
        if (election.getStatus() != Election.ElectionStatus.SCHEDULED) {
            throw new RuntimeException("Election must be scheduled to start");
        }
        
        election.setStatus(Election.ElectionStatus.ACTIVE);
        election.setUpdatedAt(LocalDateTime.now());
        return electionRepository.save(election);
    }
    
    public Election stopElection(Long id, User stoppedBy) {
        Election election = electionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Election not found"));
        
        if (election.getStatus() != Election.ElectionStatus.ACTIVE) {
            throw new RuntimeException("Only active elections can be stopped");
        }
        
        election.setStatus(Election.ElectionStatus.COMPLETED);
        election.setUpdatedAt(LocalDateTime.now());
        return electionRepository.save(election);
    }
    
    public Long getVoteCount(Long electionId) {
        Election election = electionRepository.findById(electionId)
            .orElseThrow(() -> new RuntimeException("Election not found"));
        return voteRepository.countValidVotesByElection(election);
    }
    
    public Long getCandidateCount(Long electionId) {
        return candidateRepository.countActiveByElectionId(electionId);
    }
    
    public List<Election> searchElections(String keyword) {
        return electionRepository.findByKeyword(keyword);
    }
}