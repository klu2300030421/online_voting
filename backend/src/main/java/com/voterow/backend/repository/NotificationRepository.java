package com.voterow.backend.repository;

import com.voterow.backend.model.Notification;
import com.voterow.backend.model.Election;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    List<Notification> findByStatusOrderByCreatedAtDesc(Notification.NotificationStatus status);
    
    List<Notification> findByElectionOrderByCreatedAtDesc(Election election);
    
    List<Notification> findByTypeOrderByCreatedAtDesc(Notification.NotificationType type);
    
    @Query("SELECT n FROM Notification n WHERE n.createdAt BETWEEN :startDate AND :endDate ORDER BY n.createdAt DESC")
    List<Notification> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.status = :status")
    Long countByStatus(@Param("status") Notification.NotificationStatus status);
    
    @Query("SELECT n FROM Notification n WHERE n.status = 'PENDING' AND n.createdAt < :cutoffTime")
    List<Notification> findPendingNotificationsOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);
}