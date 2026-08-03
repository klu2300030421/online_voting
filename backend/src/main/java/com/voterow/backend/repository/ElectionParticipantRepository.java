package com.voterow.backend.repository;

import com.voterow.backend.model.ElectionParticipant;
import com.voterow.backend.model.Election;
import com.voterow.backend.model.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ElectionParticipantRepository extends JpaRepository<ElectionParticipant, Long> {
    
    List<ElectionParticipant> findByElection(Election election);
    
    List<ElectionParticipant> findByElectionAndIsActiveTrue(Election election);
    
    List<ElectionParticipant> findByCandidate(Candidate candidate);
    
    Optional<ElectionParticipant> findByElectionAndCandidate(Election election, Candidate candidate);
    
    boolean existsByElectionAndCandidate(Election election, Candidate candidate);
    
    // Legacy methods for backward compatibility
    List<ElectionParticipant> findByElectionId(Long electionId);
    
    List<ElectionParticipant> findByCandidateId(Long candidateId);
    
    boolean existsByElectionIdAndCandidateId(Long electionId, Long candidateId);
    
    // NOTE: methods that operate on entities (existsByElectionAndCandidate, findByElectionAndIsActiveTrue)
    // are declared earlier in this interface. The id-based legacy methods above are kept for
    // backward compatibility, but the duplicate entity-based methods were removed to fix
    // duplicate-method compilation errors.
}