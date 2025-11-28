package com.voterow.backend.service;

import com.voterow.backend.model.*;
import com.voterow.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ElectionManagementService {
    
    @Autowired
    private ElectionRepository electionRepository;
    
    @Autowired
    private ElectionParticipantRepository electionParticipantRepository;
    
    @Autowired
    private ElectionVoterRepository electionVoterRepository;
    
    @Autowired
    private CandidateRepository candidateRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private VoteRepository voteRepository;
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    /**
     * Assign approved candidates to an election as participants
     */
    public void assignCandidatesToElection(Long electionId, List<Long> candidateIds) {
        Optional<Election> electionOpt = electionRepository.findById(electionId);
        if (!electionOpt.isPresent()) {
            throw new RuntimeException("Election not found");
        }
        
        Election election = electionOpt.get();
        
        for (Long candidateId : candidateIds) {
            Optional<Candidate> candidateOpt = candidateRepository.findById(candidateId);
            if (candidateOpt.isPresent() && candidateOpt.get().getStatus() == Candidate.CandidateStatus.APPROVED) {
                
                // Check if candidate is already assigned to this election
                Optional<ElectionParticipant> existingParticipant = 
                    electionParticipantRepository.findByElectionAndCandidate(election, candidateOpt.get());
                    
                if (!existingParticipant.isPresent()) {
                    ElectionParticipant participant = new ElectionParticipant();
                    participant.setElection(election);
                    participant.setCandidate(candidateOpt.get());
                    participant.setAssignedAt(LocalDateTime.now());
                    participant.setIsActive(true);
                    
                    electionParticipantRepository.save(participant);
                    
                    // Create audit log
                    AuditLog auditLog = new AuditLog();
                    auditLog.setUser(candidateOpt.get().getUser());
                    auditLog.setAction("CANDIDATE_ASSIGNED_TO_ELECTION");
                    auditLog.setEntityType("ElectionParticipant");
                    auditLog.setEntityId(participant.getId());
                    auditLog.setDetails("Candidate assigned to election: " + election.getTitle());
                    auditLog.setLogLevel(AuditLog.LogLevel.INFO);
                    auditLog.setTimestamp(LocalDateTime.now());
                    auditLogRepository.save(auditLog);
                    
                    // Create notification for candidate
                    Notification notification = new Notification();
                    notification.setUser(candidateOpt.get().getUser());
                    notification.setTitle("Assigned to Election");
                    notification.setMessage("You have been assigned as a candidate for election: " + election.getTitle());
                    notification.setNotificationType(Notification.NotificationType.ELECTION_ANNOUNCEMENT);
                    notification.setPriority(Notification.Priority.NORMAL);
                    notification.setIsRead(false);
                    notification.setCreatedAt(LocalDateTime.now());
                    notificationRepository.save(notification);
                }
            }
        }
    }
    
    /**
     * Register eligible voters for an election
     */
    public void registerVotersForElection(Long electionId, List<Long> voterIds) {
        Optional<Election> electionOpt = electionRepository.findById(electionId);
        if (!electionOpt.isPresent()) {
            throw new RuntimeException("Election not found");
        }
        
        Election election = electionOpt.get();
        
        for (Long voterId : voterIds) {
            Optional<User> voterOpt = userRepository.findById(voterId);
            if (voterOpt.isPresent() && voterOpt.get().getUserType() == UserType.ROLE_VOTER) {
                
                // Check if voter is already registered for this election
                Optional<ElectionVoter> existingVoter = 
                    electionVoterRepository.findByElectionAndVoter(election, voterOpt.get());
                    
                if (!existingVoter.isPresent()) {
                    ElectionVoter electionVoter = new ElectionVoter();
                    electionVoter.setElection(election);
                    electionVoter.setVoter(voterOpt.get());
                    electionVoter.setRegisteredAt(LocalDateTime.now());
                    electionVoter.setIsEligible(true);
                    
                    electionVoterRepository.save(electionVoter);
                    
                    // Create audit log
                    AuditLog auditLog = new AuditLog();
                    auditLog.setUser(voterOpt.get());
                    auditLog.setAction("VOTER_REGISTERED_FOR_ELECTION");
                    auditLog.setEntityType("ElectionVoter");
                    auditLog.setEntityId(electionVoter.getId());
                    auditLog.setDetails("Voter registered for election: " + election.getTitle());
                    auditLog.setLogLevel(AuditLog.LogLevel.INFO);
                    auditLog.setTimestamp(LocalDateTime.now());
                    auditLogRepository.save(auditLog);
                    
                    // Create notification for voter
                    Notification notification = new Notification();
                    notification.setUser(voterOpt.get());
                    notification.setTitle("Election Registration");
                    notification.setMessage("You are now eligible to vote in election: " + election.getTitle());
                    notification.setNotificationType(Notification.NotificationType.VOTING_REMINDER);
                    notification.setPriority(Notification.Priority.HIGH);
                    notification.setIsRead(false);
                    notification.setCreatedAt(LocalDateTime.now());
                    notificationRepository.save(notification);
                }
            }
        }
    }
    
    /**
     * Get all participants (candidates) for an election
     */
    public List<ElectionParticipant> getElectionParticipants(Long electionId) {
        Optional<Election> electionOpt = electionRepository.findById(electionId);
        if (!electionOpt.isPresent()) {
            throw new RuntimeException("Election not found");
        }
        
        return electionParticipantRepository.findByElectionAndIsActiveTrue(electionOpt.get());
    }
    
    /**
     * Get all eligible voters for an election
     */
    public List<ElectionVoter> getElectionVoters(Long electionId) {
        Optional<Election> electionOpt = electionRepository.findById(electionId);
        if (!electionOpt.isPresent()) {
            throw new RuntimeException("Election not found");
        }
        
        return electionVoterRepository.findByElectionAndIsEligibleTrue(electionOpt.get());
    }
    
    /**
     * Record a vote in the database
     */
    public Vote castVote(Long electionId, Long voterId, Long candidateId) {
        // Validate election
        Optional<Election> electionOpt = electionRepository.findById(electionId);
        if (!electionOpt.isPresent()) {
            throw new RuntimeException("Election not found");
        }
        
        Election election = electionOpt.get();
        if (election.getStatus() != Election.ElectionStatus.ACTIVE) {
            throw new RuntimeException("Election is not active");
        }
        
        // Validate voter
        Optional<User> voterOpt = userRepository.findById(voterId);
        if (!voterOpt.isPresent()) {
            throw new RuntimeException("Voter not found");
        }
        
        User voter = voterOpt.get();
        
        // Check if voter is eligible for this election
        Optional<ElectionVoter> electionVoterOpt = 
            electionVoterRepository.findByElectionAndVoter(election, voter);
        if (!electionVoterOpt.isPresent() || !electionVoterOpt.get().getIsEligible()) {
            throw new RuntimeException("Voter is not eligible for this election");
        }
        
        // Validate candidate
        Optional<Candidate> candidateOpt = candidateRepository.findById(candidateId);
        if (!candidateOpt.isPresent()) {
            throw new RuntimeException("Candidate not found");
        }
        
        Candidate candidate = candidateOpt.get();
        
        // Check if candidate is participating in this election
        Optional<ElectionParticipant> participantOpt = 
            electionParticipantRepository.findByElectionAndCandidate(election, candidate);
        if (!participantOpt.isPresent() || !participantOpt.get().getIsActive()) {
            throw new RuntimeException("Candidate is not participating in this election");
        }
        
        // Check if voter has already voted
        if (voteRepository.existsByElectionAndVoter(election, voter)) {
            throw new RuntimeException("Voter has already voted in this election");
        }
        
        // Create and save vote
        Vote vote = new Vote();
        vote.setElection(election);
        vote.setVoter(voter);
        vote.setCandidate(candidate);
        vote.setVotedAt(LocalDateTime.now());
        vote.setVoteHash(generateVoteHash(voterId, candidateId, electionId));
        
        Vote savedVote = voteRepository.save(vote);
        
        // Create audit log
        AuditLog auditLog = new AuditLog();
        auditLog.setUser(voter);
        auditLog.setAction("VOTE_CAST");
        auditLog.setEntityType("Vote");
        auditLog.setEntityId(savedVote.getId());
        auditLog.setDetails("Vote cast in election: " + election.getTitle());
        auditLog.setLogLevel(AuditLog.LogLevel.INFO);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(auditLog);
        
        return savedVote;
    }
    
    /**
     * Get election results with vote counts
     */
    public List<Object[]> getElectionResults(Long electionId) {
        Optional<Election> electionOpt = electionRepository.findById(electionId);
        if (!electionOpt.isPresent()) {
            throw new RuntimeException("Election not found");
        }
        
        return voteRepository.getVoteCountsByElection(electionId);
    }
    
    /**
     * Auto-register eligible voters based on criteria
     */
    public void autoRegisterEligibleVoters(Long electionId) {
        Optional<Election> electionOpt = electionRepository.findById(electionId);
        if (!electionOpt.isPresent()) {
            throw new RuntimeException("Election not found");
        }
        
        Election election = electionOpt.get();
        
        // Find eligible voters based on age and verification status
        List<User> eligibleVoters = userRepository.findEligibleVoters(election.getMinAge());
        
        for (User voter : eligibleVoters) {
            // Check if voter is not already registered
            Optional<ElectionVoter> existingVoter = 
                electionVoterRepository.findByElectionAndVoter(election, voter);
                
            if (!existingVoter.isPresent()) {
                ElectionVoter electionVoter = new ElectionVoter();
                electionVoter.setElection(election);
                electionVoter.setVoter(voter);
                electionVoter.setRegisteredAt(LocalDateTime.now());
                electionVoter.setIsEligible(true);
                
                electionVoterRepository.save(electionVoter);
            }
        }
    }
    
    private String generateVoteHash(Long voterId, Long candidateId, Long electionId) {
        return String.valueOf(Long.valueOf(voterId + candidateId + electionId + System.currentTimeMillis()).hashCode());
    }
}