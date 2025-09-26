package com.voterow.backend.repository;

import com.voterow.backend.model.Election;
import com.voterow.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ElectionRepository extends JpaRepository<Election, Long> {
    
    List<Election> findByStatusOrderByCreatedAtDesc(Election.ElectionStatus status);
    
    List<Election> findByTypeOrderByCreatedAtDesc(Election.ElectionType type);
    
    List<Election> findByIsActiveTrueOrderByCreatedAtDesc();
    
    List<Election> findByCreatedByOrderByCreatedAtDesc(User createdBy);
    
    @Query("SELECT e FROM Election e WHERE e.startDate <= :now AND e.endDate >= :now AND e.status = 'ACTIVE'")
    List<Election> findCurrentlyActiveElections(@Param("now") LocalDateTime now);
    
    @Query("SELECT e FROM Election e WHERE e.startDate > :now AND e.status = 'SCHEDULED'")
    List<Election> findUpcomingElections(@Param("now") LocalDateTime now);
    
    @Query("SELECT e FROM Election e WHERE e.endDate < :now AND e.status IN ('ACTIVE', 'COMPLETED')")
    List<Election> findCompletedElections(@Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(e) FROM Election e WHERE e.status = :status")
    Long countByStatus(@Param("status") Election.ElectionStatus status);
    
    @Query("SELECT e FROM Election e WHERE e.title LIKE %:keyword% OR e.description LIKE %:keyword%")
    List<Election> findByKeyword(@Param("keyword") String keyword);
}