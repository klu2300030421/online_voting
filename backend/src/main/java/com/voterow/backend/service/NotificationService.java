package com.voterow.backend.service;

import com.voterow.backend.model.Notification;
import com.voterow.backend.model.Election;
import com.voterow.backend.model.User;
import com.voterow.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final AuditLogService auditLogService;
    
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }
    
    public List<Notification> getNotificationsByStatus(Notification.NotificationStatus status) {
        return notificationRepository.findByStatusOrderByCreatedAtDesc(status);
    }
    
    public List<Notification> getNotificationsByElection(Election election) {
        return notificationRepository.findByElectionOrderByCreatedAtDesc(election);
    }
    
    public Optional<Notification> getNotificationById(Long id) {
        return notificationRepository.findById(id);
    }
    
    public Notification createNotification(Notification notification, User createdBy) {
        notification.setSentBy(createdBy);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setStatus(Notification.NotificationStatus.PENDING);
        
        Notification saved = notificationRepository.save(notification);
        
        auditLogService.logAction(createdBy, "CREATE_NOTIFICATION", "Notification", saved.getId(), 
            "Created notification: " + notification.getTitle());
        
        return saved;
    }
    
    public void sendElectionNotification(Election election, String title, String message, 
                                       Notification.NotificationType type, User sentBy) {
        // This would integrate with actual SMS/Email service
        // For now, we'll just create the notification record
        
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setElection(election);
        notification.setSentBy(sentBy);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setStatus(Notification.NotificationStatus.PENDING);
        
        Notification saved = notificationRepository.save(notification);
        
        // Here you would integrate with SMS/Email providers
        // For demo purposes, we'll mark as sent immediately
        markAsSent(saved.getId());
        
        auditLogService.logAction(sentBy, "SEND_ELECTION_NOTIFICATION", "Notification", saved.getId(), 
            "Sent election notification: " + title + " for election: " + election.getTitle());
    }
    
    public void sendBulkNotification(String title, String message, 
                                   List<String> phoneNumbers, List<String> emailAddresses,
                                   Notification.NotificationType type, User sentBy) {
        
        // Create individual notifications for each recipient
        for (String phone : phoneNumbers) {
            if (type == Notification.NotificationType.SMS || type == Notification.NotificationType.BOTH) {
                Notification notification = new Notification();
                notification.setTitle(title);
                notification.setMessage(message);
                notification.setType(Notification.NotificationType.SMS);
                notification.setRecipientPhone(phone);
                notification.setSentBy(sentBy);
                notification.setCreatedAt(LocalDateTime.now());
                notification.setStatus(Notification.NotificationStatus.PENDING);
                
                notificationRepository.save(notification);
            }
        }
        
        for (String email : emailAddresses) {
            if (type == Notification.NotificationType.EMAIL || type == Notification.NotificationType.BOTH) {
                Notification notification = new Notification();
                notification.setTitle(title);
                notification.setMessage(message);
                notification.setType(Notification.NotificationType.EMAIL);
                notification.setRecipientEmail(email);
                notification.setSentBy(sentBy);
                notification.setCreatedAt(LocalDateTime.now());
                notification.setStatus(Notification.NotificationStatus.PENDING);
                
                notificationRepository.save(notification);
            }
        }
        
        auditLogService.logAction(sentBy, "SEND_BULK_NOTIFICATION", "Notification", null, 
            "Sent bulk notification: " + title + " to " + 
            (phoneNumbers.size() + emailAddresses.size()) + " recipients");
    }
    
    public Notification markAsSent(Long id) {
        Notification notification = notificationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Notification not found"));
        
        notification.setStatus(Notification.NotificationStatus.SENT);
        notification.setSentAt(LocalDateTime.now());
        
        return notificationRepository.save(notification);
    }
    
    public Notification markAsFailed(Long id, String errorMessage) {
        Notification notification = notificationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Notification not found"));
        
        notification.setStatus(Notification.NotificationStatus.FAILED);
        notification.setErrorMessage(errorMessage);
        
        return notificationRepository.save(notification);
    }
    
    public List<Notification> getPendingNotifications() {
        return notificationRepository.findByStatusOrderByCreatedAtDesc(Notification.NotificationStatus.PENDING);
    }
    
    public void processOldPendingNotifications() {
        // Mark notifications older than 1 hour as failed
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(1);
        List<Notification> oldNotifications = notificationRepository.findPendingNotificationsOlderThan(cutoffTime);
        
        for (Notification notification : oldNotifications) {
            markAsFailed(notification.getId(), "Timeout - notification not processed within allowed time");
        }
    }
    
    public Long getNotificationCountByStatus(Notification.NotificationStatus status) {
        return notificationRepository.countByStatus(status);
    }
}