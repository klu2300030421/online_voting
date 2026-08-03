package com.voterow.backend.repository;

import com.voterow.backend.model.Vote;
import com.voterow.backend.model.Election;
import com.voterow.backend.model.User;
import com.voterow.backend.model.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {
    
    // Check if voter already voted in election
    Optional<Vote> findByVoterAndElection(User voter, Election election);
    
    // Check if voter already voted in election (boolean method)
    boolean existsByElectionAndVoter(Election election, User voter);
    
    // Get all votes for an election
    List<Vote> findByElection(Election election);
    
    // Count votes for a candidate in an election
    long countByCandidateAndElection(Candidate candidate, Election election);
    
    // Count valid votes by election
    @Query("SELECT COUNT(v) FROM Vote v WHERE v.election = ?1")
    long countValidVotesByElection(Election election);
    
    // Get vote count by election
    @Query("SELECT COUNT(v) FROM Vote v WHERE v.election = ?1")
    long countVotesByElection(Election election);
    
    // Get election results with vote counts by candidate
    @Query("SELECT c.user.fullName, c.partyName, COUNT(v.id) as voteCount " +
           "FROM Vote v JOIN v.candidate c " +
           "WHERE v.election.id = :electionId " +
           "GROUP BY c.id, c.user.fullName, c.partyName " +
           "ORDER BY COUNT(v.id) DESC")
    List<Object[]> getVoteCountsByElection(Long electionId);
    
    @Query("SELECT c.id, c.user.fullName, c.partyName, COUNT(v) FROM Vote v " +
           "JOIN v.candidate c WHERE v.election.id = :electionId " +
           "GROUP BY c.id, c.user.fullName, c.partyName ORDER BY COUNT(v) DESC")
    List<Object[]> countVotesByCandidate(@Param("electionId") Long electionId);
    
    // Delete all votes by a specific voter (for cascading delete)
    void deleteByVoter(User voter);
    
    @Query("DELETE FROM Vote v WHERE v.voter.id = :voterId")
    @Modifying
    void deleteByVoterId(@Param("voterId") Long voterId);
    
    // Get all votes by a specific voter
    List<Vote> findByVoter(User voter);
}