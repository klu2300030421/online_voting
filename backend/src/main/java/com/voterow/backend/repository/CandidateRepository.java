package com.voterow.backend.repository;

import com.voterow.backend.model.Candidate;
import com.voterow.backend.model.Election;
import com.voterow.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    
    List<Candidate> findByElection(Election election);
    
    List<Candidate> findByElectionAndIsActiveTrue(Election election);
    
    List<Candidate> findByUser(User user);
    
    List<Candidate> findByStatus(Candidate.CandidateStatus status);
    
    @Query("SELECT c FROM Candidate c WHERE c.election.id = :electionId AND c.status = :status")
    List<Candidate> findByElectionIdAndStatus(@Param("electionId") Long electionId, 
                                            @Param("status") Candidate.CandidateStatus status);
    
    @Query("SELECT COUNT(c) FROM Candidate c WHERE c.election.id = :electionId AND c.isActive = true")
    Long countActiveByElectionId(@Param("electionId") Long electionId);
    
    boolean existsByUserAndElection(User user, Election election);
}