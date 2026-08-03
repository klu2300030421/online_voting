package com.voterow.backend.controller;

import com.voterow.backend.model.Notification;
import com.voterow.backend.model.User;
import com.voterow.backend.repository.NotificationRepository;
import com.voterow.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Get notifications for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getUserNotifications(@PathVariable Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        List<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(userOpt.get());
        return ResponseEntity.ok(notifications);
    }
    
    /**
     * Get unread notifications for a user
     */
    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(@PathVariable Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        List<Notification> notifications = notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(userOpt.get());
        return ResponseEntity.ok(notifications);
    }
    
    /**
     * Mark notification as read
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long notificationId) {
        Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
        if (!notificationOpt.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Notification not found");
            return ResponseEntity.badRequest().body(response);
        }
        
        Notification notification = notificationOpt.get();
        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Notification marked as read");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Mark all notifications as read for a user
     */
    @PutMapping("/user/{userId}/mark-all-read")
    public ResponseEntity<Map<String, Object>> markAllAsRead(@PathVariable Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "User not found");
            return ResponseEntity.badRequest().body(response);
        }
        
        List<Notification> unreadNotifications = notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(userOpt.get());
        LocalDateTime now = LocalDateTime.now();
        
        for (Notification notification : unreadNotifications) {
            notification.setIsRead(true);
            notification.setReadAt(now);
        }
        
        notificationRepository.saveAll(unreadNotifications);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", unreadNotifications.size() + " notifications marked as read");
        response.put("count", unreadNotifications.size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Delete notification
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Map<String, Object>> deleteNotification(@PathVariable Long notificationId) {
        Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
        if (!notificationOpt.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Notification not found");
            return ResponseEntity.badRequest().body(response);
        }
        
        notificationRepository.deleteById(notificationId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Notification deleted successfully");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Create a new notification
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createNotification(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String title = (String) request.get("title");
            String message = (String) request.get("message");
            String type = (String) request.get("type");
            String priority = (String) request.get("priority");
            
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "User not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            Notification notification = new Notification();
            notification.setUser(userOpt.get());
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setNotificationType(Notification.NotificationType.valueOf(type));
            notification.setPriority(Notification.Priority.valueOf(priority));
            notification.setIsRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            
            Notification savedNotification = notificationRepository.save(notification);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notification created successfully");
            response.put("notificationId", savedNotification.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to create notification");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}