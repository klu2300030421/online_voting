package com.voterow.backend.controller;

import com.voterow.backend.model.AuditLog;
import com.voterow.backend.model.User;
import com.voterow.backend.repository.AuditLogRepository;
import com.voterow.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/audit")
public class AuditLogController {
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Get all audit logs with pagination
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        Page<AuditLog> auditLogPage = auditLogRepository.findAll(pageRequest);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", auditLogPage.getContent());
        response.put("page", auditLogPage.getNumber());
        response.put("size", auditLogPage.getSize());
        response.put("totalElements", auditLogPage.getTotalElements());
        response.put("totalPages", auditLogPage.getTotalPages());
        response.put("first", auditLogPage.isFirst());
        response.put("last", auditLogPage.isLast());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get audit logs for a specific user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AuditLog>> getUserAuditLogs(@PathVariable Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        List<AuditLog> auditLogs = auditLogRepository.findByUserOrderByTimestampDesc(userOpt.get());
        return ResponseEntity.ok(auditLogs);
    }
    
    /**
     * Get audit logs by action type
     */
    @GetMapping("/action/{action}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByAction(@PathVariable String action) {
        List<AuditLog> auditLogs = auditLogRepository.findByActionOrderByTimestampDesc(action);
        return ResponseEntity.ok(auditLogs);
    }
    
    /**
     * Get audit logs by entity type
     */
    @GetMapping("/entity/{entityType}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByEntityType(@PathVariable String entityType) {
        List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeOrderByTimestampDesc(entityType);
        return ResponseEntity.ok(auditLogs);
    }
    
    /**
     * Get audit logs within a date range
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<AuditLog>> getAuditLogsByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        
        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);
        
        List<AuditLog> auditLogs = auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(start, end);
        return ResponseEntity.ok(auditLogs);
    }
    
    /**
     * Get audit log statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAuditLogStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Total count
        long totalLogs = auditLogRepository.count();
        stats.put("totalLogs", totalLogs);
        
        // Count by log level
        for (AuditLog.LogLevel level : AuditLog.LogLevel.values()) {
            long count = auditLogRepository.countByLogLevel(level);
            stats.put(level.name().toLowerCase() + "Count", count);
        }
        
        // Recent activity (last 24 hours)
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        long recentActivity = auditLogRepository.countByTimestampAfter(yesterday);
        stats.put("recentActivity", recentActivity);
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Create a manual audit log entry
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createAuditLog(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String action = (String) request.get("action");
            String entityType = (String) request.get("entityType");
            Long entityId = request.get("entityId") != null ? Long.valueOf(request.get("entityId").toString()) : null;
            String details = (String) request.get("details");
            String logLevel = (String) request.get("logLevel");
            
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "User not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            AuditLog auditLog = new AuditLog();
            auditLog.setUser(userOpt.get());
            auditLog.setAction(action);
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setDetails(details);
            auditLog.setLogLevel(AuditLog.LogLevel.valueOf(logLevel));
            auditLog.setTimestamp(LocalDateTime.now());
            
            AuditLog savedLog = auditLogRepository.save(auditLog);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Audit log created successfully");
            response.put("auditLogId", savedLog.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to create audit log");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}