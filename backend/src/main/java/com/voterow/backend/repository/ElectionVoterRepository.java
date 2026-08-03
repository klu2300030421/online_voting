package com.voterow.backend.repository;

import com.voterow.backend.model.ElectionVoter;
import com.voterow.backend.model.Election;
import com.voterow.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ElectionVoterRepository extends JpaRepository<ElectionVoter, Long> {
    
    List<ElectionVoter> findByElection(Election election);
    
    List<ElectionVoter> findByElectionAndIsEligibleTrue(Election election);
    
    List<ElectionVoter> findByVoter(User voter);
    
    Optional<ElectionVoter> findByElectionAndVoter(Election election, User voter);
    
    boolean existsByElectionAndVoter(Election election, User voter);
    
    // Legacy methods for backward compatibility
    List<ElectionVoter> findByElectionId(Long electionId);
    
    List<ElectionVoter> findByVoterId(Long voterId);
    
    boolean existsByElectionIdAndVoterId(Long electionId, Long voterId);
    
    // Delete all election voter registrations for a specific voter (for cascading delete)
    void deleteByVoter(User voter);
    
    @Query("DELETE FROM ElectionVoter ev WHERE ev.voter.id = :voterId")
    @Modifying
    void deleteByVoterId(@Param("voterId") Long voterId);
}