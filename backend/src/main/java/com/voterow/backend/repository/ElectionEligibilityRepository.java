package com.voterow.backend.repository;

import com.voterow.backend.model.ElectionEligibility;
import com.voterow.backend.model.Election;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ElectionEligibilityRepository extends JpaRepository<ElectionEligibility, Long> {
    Optional<ElectionEligibility> findByElection(Election election);
}