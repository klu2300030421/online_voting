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
    
    List<Candidate> findByElectionAndStatus(Election election, Candidate.CandidateStatus status);
    
    List<Candidate> findByUser(User user);
    
    List<Candidate> findByStatus(Candidate.CandidateStatus status);
    
    @Query("SELECT c FROM Candidate c WHERE c.election.id = :electionId AND c.status = :status")
    List<Candidate> findByElectionIdAndStatus(@Param("electionId") Long electionId, 
                                            @Param("status") Candidate.CandidateStatus status);
    
    @Query("SELECT COUNT(c) FROM Candidate c WHERE c.election.id = :electionId AND c.isActive = true")
    Long countActiveByElectionId(@Param("electionId") Long electionId);
    
    boolean existsByUserAndElection(User user, Election election);
    
    @Query("SELECT c FROM Candidate c WHERE c.user.id = :userId AND c.election.id = :electionId")
    List<Candidate> findByUserIdAndElectionId(@Param("userId") Long userId, @Param("electionId") Long electionId);
    
    @Query("SELECT c FROM Candidate c WHERE c.user.id = :userId")
    List<Candidate> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT c FROM Candidate c LEFT JOIN FETCH c.user LEFT JOIN FETCH c.election WHERE c.user.id = :userId")
    List<Candidate> findByUserIdWithDetails(@Param("userId") Long userId);
}