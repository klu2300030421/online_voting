package com.voterow.backend.repository;

import com.voterow.backend.model.VoterAccess;
import com.voterow.backend.model.User;
import com.voterow.backend.model.Election;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface VoterAccessRepository extends JpaRepository<VoterAccess, Long> {
    Optional<VoterAccess> findByAccessToken(String accessToken);
    Optional<VoterAccess> findByVoterAndElection(User voter, Election election);
    List<VoterAccess> findByElectionAndIsUsedFalse(Election election);
}