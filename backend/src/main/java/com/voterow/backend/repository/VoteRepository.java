package com.voterow.backend.repository;

import com.voterow.backend.model.Vote;
import com.voterow.backend.model.Election;
import com.voterow.backend.model.User;
import com.voterow.backend.model.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {
    
    boolean existsByElectionAndVoter(Election election, User voter);
    
    List<Vote> findByElectionOrderByVotedAtDesc(Election election);
    
    List<Vote> findByVoterOrderByVotedAtDesc(User voter);
    
    List<Vote> findByCandidateOrderByVotedAtDesc(Candidate candidate);
    
    // Add missing method for counting votes by election ID
    Long countByElectionId(Long electionId);
    
    @Query("SELECT COUNT(v) FROM Vote v WHERE v.election = :election AND v.isValid = true")
    Long countValidVotesByElection(@Param("election") Election election);
    
    @Query("SELECT COUNT(v) FROM Vote v WHERE v.candidate = :candidate AND v.isValid = true")
    Long countValidVotesByCandidate(@Param("candidate") Candidate candidate);
    
    @Query("SELECT v.candidate, COUNT(v) as voteCount FROM Vote v WHERE v.election = :election AND v.isValid = true GROUP BY v.candidate ORDER BY COUNT(v) DESC")
    List<Object[]> getElectionResults(@Param("election") Election election);
    
    @Query("SELECT COUNT(v) FROM Vote v WHERE v.election = :election AND v.votedAt BETWEEN :startTime AND :endTime")
    Long countVotesByElectionAndTimeRange(@Param("election") Election election,
                                        @Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT COUNT(DISTINCT v.voter) FROM Vote v WHERE v.election = :election")
    Long countUniqueVotersByElection(@Param("election") Election election);
    
    @Query("SELECT v FROM Vote v WHERE v.voterIpAddress = :ipAddress AND v.votedAt > :since")
    List<Vote> findRecentVotesByIpAddress(@Param("ipAddress") String ipAddress, 
                                        @Param("since") LocalDateTime since);
    
    @Query("SELECT v FROM Vote v WHERE v.voter = :voter AND v.votedAt > :since")
    List<Vote> findRecentVotesByVoter(@Param("voter") User voter, 
                                    @Param("since") LocalDateTime since);
}