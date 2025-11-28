package com.voterow.backend.controller;

import com.voterow.backend.model.User;
import com.voterow.backend.model.UserType;
import com.voterow.backend.model.Election;
import com.voterow.backend.model.Candidate;
import com.voterow.backend.model.ElectionParticipant;
import com.voterow.backend.model.Vote;
import com.voterow.backend.repository.UserRepository;
import com.voterow.backend.repository.ElectionRepository;
import com.voterow.backend.repository.CandidateRepository;
import com.voterow.backend.repository.ElectionParticipantRepository;
import com.voterow.backend.repository.VoteRepository;
import com.voterow.backend.repository.ElectionVoterRepository;
import com.voterow.backend.service.ElectionWorkflowService;
import com.voterow.backend.service.AuditLogService;
import com.voterow.backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private static final String DEFAULT_PASSWORD = "temp123";
    private static final int DEFAULT_AGE = 25;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ElectionRepository electionRepository;
    
    @Autowired
    private CandidateRepository candidateRepository;
    
    @Autowired
    private ElectionParticipantRepository electionParticipantRepository;
    
    @Autowired
    private VoteRepository voteRepository;
    
    @Autowired
    private ElectionVoterRepository electionVoterRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private ElectionWorkflowService electionWorkflowService;
    
    @Autowired
    private AuditLogService auditLogService;
    
    @Autowired
    private NotificationService notificationService;
    
    // Dashboard Overview
    @GetMapping("/dashboard/overview")
    public ResponseEntity<Map<String, Object>> getDashboardOverview() {
        Map<String, Object> overview = new HashMap<>();
        overview.put("totalElections", 3);
        overview.put("totalVoters", 1250);
        overview.put("totalCandidates", 15);
        overview.put("activeElections", 1);
        return ResponseEntity.ok(overview);
    }
    
    // Basic user management with mock data
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<Map<String, Object>> users = new ArrayList<>();
        users.add(createMockUser(1L, "Admin User", "admin@voterow.com", "ADMIN", true, true));
        users.add(createMockUser(2L, "John Doe", "john@example.com", "VOTER", true, true));
        users.add(createMockUser(3L, "Jane Smith", "jane@example.com", "CANDIDATE", true, false));
        return ResponseEntity.ok(users);
    }
    
    // Voters endpoints
    @GetMapping("/voters")
    public ResponseEntity<List<Map<String, Object>>> getVoters() {
        try {
            // Fetch all voters from database
            List<User> voterUsers = userRepository.findByUserType(UserType.ROLE_VOTER);
            
            List<Map<String, Object>> voters = new ArrayList<>();
            for (User user : voterUsers) {
                Map<String, Object> voterMap = new HashMap<>();
                voterMap.put("id", user.getId());
                voterMap.put("fullName", user.getFullName());
                voterMap.put("email", user.getEmail());
                voterMap.put("age", user.getAge());
                voterMap.put("phoneNumber", user.getPhoneNumber());
                voterMap.put("userType", user.getUserType().toString());
                voterMap.put("role", "VOTER");
                voterMap.put("isActive", user.getIsActive());
                voterMap.put("isVerified", user.getIsVerified());
                voterMap.put("registeredAt", user.getCreatedAt());
                voterMap.put("status", user.getIsActive() ? "Active" : "Inactive");
                voters.add(voterMap);
            }
            
            System.out.println("Found " + voters.size() + " voters in database");
            return ResponseEntity.ok(voters);
            
        } catch (Exception e) {
            logger.error("Error fetching voters", e);
            e.printStackTrace();
            
            // Return empty list on error
            return ResponseEntity.ok(new ArrayList<>());
        }
    }
    
    // Add new voter
    @PostMapping("/voters")
    public ResponseEntity<Map<String, Object>> addVoter(@RequestBody Map<String, Object> voterData) {
        // Log the received data for debugging
        logger.info("Processing voter registration request");
        
        try {
            // Check if email already exists
            String email = (String) voterData.get("email");
            if (userRepository.findByEmail(email).isPresent()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Email already exists");
                errorResponse.put("message", "A user with this email already exists");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Create new User entity
            User newVoter = new User();
            newVoter.setFullName((String) voterData.get("fullName"));
            newVoter.setEmail(email);
            
            // Set a default password (in production, you'd generate and send this)
            newVoter.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD)); // Default password
            
            // Handle age conversion
            Object ageObj = voterData.get("age");
            if (ageObj != null) {
                if (ageObj instanceof String) {
                    newVoter.setAge(Integer.parseInt((String) ageObj));
                } else if (ageObj instanceof Integer) {
                    newVoter.setAge((Integer) ageObj);
                }
            }
            
            newVoter.setPhoneNumber((String) voterData.get("phone"));
            newVoter.setUserType(UserType.ROLE_VOTER);
            newVoter.setIsActive(true);
            newVoter.setIsVerified(false);
            newVoter.setTwoFactorEnabled(false);
            newVoter.setCreatedAt(LocalDateTime.now());
            newVoter.setUpdatedAt(LocalDateTime.now());
            
            // Save to database
            User savedVoter = userRepository.save(newVoter);
            
            // Create response
            Map<String, Object> result = new HashMap<>();
            result.put("id", savedVoter.getId());
            result.put("fullName", savedVoter.getFullName());
            result.put("email", savedVoter.getEmail());
            result.put("age", savedVoter.getAge());
            result.put("phone", savedVoter.getPhoneNumber());
            result.put("userType", savedVoter.getUserType().toString());
            result.put("role", "VOTER");
            result.put("isActive", savedVoter.getIsActive());
            result.put("isVerified", savedVoter.getIsVerified());
            result.put("createdAt", savedVoter.getCreatedAt());
            
            logger.info("Successfully created voter with ID: {}", savedVoter.getId());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error creating voter: {}", e.getMessage());
            e.printStackTrace();
            
            // Return error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to create voter");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    // Verify voters
    @PostMapping("/voters/verify")
    public ResponseEntity<Map<String, String>> verifyVoters() {
        // In a real app, this would update verification status in the database
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Voter verification process initiated");
        return ResponseEntity.ok(response);
    }
    
    // Import CSV of voters
    @PostMapping("/voters/import")
    public ResponseEntity<Map<String, String>> importVoters(@RequestParam(value = "file", required = false) MultipartFile file) {
        if (file == null || file.isEmpty()) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "No file provided or file is empty");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        // In a real app, this would parse CSV and save to database
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Voters imported successfully");
        return ResponseEntity.ok(response);
    }
    
    // Export voters list as CSV
    @GetMapping("/voters/export")
    public ResponseEntity<String> exportVoters() {
        List<User> voters = userRepository.findByUserType(UserType.ROLE_VOTER);
        StringBuilder csv = new StringBuilder();
        csv.append("id,fullName,email,age,status,verified\n");
        for (User voter : voters) {
            csv.append(voter.getId()).append(",")
                .append(voter.getFullName()).append(",")
                .append(voter.getEmail()).append(",")
                .append(voter.getAge() != null ? voter.getAge() : "").append(",")
                .append(Boolean.TRUE.equals(voter.getIsActive()) ? "Active" : "Inactive").append(",")
                .append(Boolean.TRUE.equals(voter.getIsVerified()) ? "Yes" : "No").append("\n");
        }
        return ResponseEntity
            .ok()
            .header("Content-Type", "text/csv")
            .header("Content-Disposition", "attachment; filename=voters.csv")
            .body(csv.toString());
    }
    
    // Test endpoint to verify controller is working
    @GetMapping("/voters/{id}/test")
    public ResponseEntity<Map<String, Object>> testVoterEndpoint(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Test endpoint working for voter ID: " + id);
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
    
    // Debug endpoint to check if DELETE mapping exists
    @GetMapping("/debug/endpoints")
    public ResponseEntity<Map<String, Object>> debugEndpoints() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "AdminController is active");
        response.put("deleteEndpoint", "DELETE /api/admin/voters/{id} is mapped");
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
    
    // Alternative delete endpoint
    @PostMapping("/voters/{id}/delete")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteVoterAlt(@PathVariable Long id) {
        logger.info("Alternative DELETE endpoint called for voter ID: {}", id);
        try {
            Optional<User> userOpt = userRepository.findById(id);
            if (!userOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Voter not found");
                return ResponseEntity.notFound().build();
            }
            
            User user = userOpt.get();
            if (!UserType.ROLE_VOTER.equals(user.getUserType())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "User is not a voter");
                return ResponseEntity.badRequest().body(response);
            }
            
            userRepository.deleteById(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Voter deleted successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error deleting voter: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to delete voter: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    // Delete voter
    @DeleteMapping("/voters/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteVoter(@PathVariable Long id) {
        logger.info("=== DELETE VOTER ENDPOINT CALLED ===");
        logger.info("DELETE request received for voter ID: {}", id);
        logger.info("Request mapping: /api/admin/voters/{}", id);
        logger.info("Deleting voter with ID: {}", id);
        try {
            Optional<User> userOpt = userRepository.findById(id);
            if (!userOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Voter not found");
                return ResponseEntity.notFound().build();
            }
            
            User user = userOpt.get();
            
            // Check if user is actually a voter
            if (!UserType.ROLE_VOTER.equals(user.getUserType())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "User is not a voter");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Delete the user (cascade deletes will handle related records automatically)
            userRepository.deleteById(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Voter deleted successfully");
            logger.info("Successfully deleted voter with ID: {}", id);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error deleting voter", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to delete voter: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    // Update voter
    @PutMapping("/voters/{id}")
    public ResponseEntity<Map<String, Object>> updateVoter(@PathVariable Long id, @RequestBody Map<String, Object> voterData) {
        try {
            User user = userRepository.findById(id).orElse(null);
            if (user == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Voter not found");
                return ResponseEntity.notFound().build();
            }
            
            // Update voter fields
            if (voterData.containsKey("fullName")) {
                user.setFullName((String) voterData.get("fullName"));
            }
            if (voterData.containsKey("email")) {
                user.setEmail((String) voterData.get("email"));
            }
            if (voterData.containsKey("age")) {
                user.setAge(Integer.parseInt(voterData.get("age").toString()));
            }
            if (voterData.containsKey("phoneNumber")) {
                user.setPhoneNumber((String) voterData.get("phoneNumber"));
            }
            if (voterData.containsKey("isActive")) {
                user.setIsActive((Boolean) voterData.get("isActive"));
            }
            if (voterData.containsKey("isVerified")) {
                user.setIsVerified((Boolean) voterData.get("isVerified"));
            }
            
            userRepository.save(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Voter updated successfully");
            response.put("id", user.getId());
            response.put("fullName", user.getFullName());
            response.put("email", user.getEmail());
            response.put("age", user.getAge());
            response.put("isActive", user.getIsActive());
            response.put("isVerified", user.getIsVerified());
            
            logger.info("Updated voter: {}", user.getFullName());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error updating voter", e);
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update voter: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    // Toggle voter verification status
    @PutMapping("/voters/{id}/toggle-verification")
    public ResponseEntity<Map<String, Object>> toggleVoterVerification(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        try {
            User user = userRepository.findById(id).orElse(null);
            if (user == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Voter not found");
                return ResponseEntity.notFound().build();
            }
            
            Boolean isVerified = (Boolean) data.get("isVerified");
            user.setIsVerified(isVerified);
            userRepository.save(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Voter verification status updated successfully");
            response.put("isVerified", user.getIsVerified());
            
            logger.info("Toggled verification for voter: {} to {}", user.getFullName(), isVerified);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error toggling voter verification", e);
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update verification status: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    // Toggle voter active status
    @PutMapping("/voters/{id}/toggle-status")
    public ResponseEntity<Map<String, Object>> toggleVoterStatus(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        try {
            User user = userRepository.findById(id).orElse(null);
            if (user == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Voter not found");
                return ResponseEntity.notFound().build();
            }
            
            Boolean isActive = (Boolean) data.get("isActive");
            user.setIsActive(isActive);
            userRepository.save(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Voter status updated successfully");
            response.put("isActive", user.getIsActive());
            
            logger.info("Toggled status for voter: {} to {}", user.getFullName(), isActive ? "Active" : "Inactive");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error toggling voter status", e);
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update voter status: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @GetMapping("/candidates")
    public ResponseEntity<List<Map<String, Object>>> getCandidates() {
        try {
            // Return candidates from User table (created by admin) and Candidate table (applications)
            List<Map<String, Object>> result = new ArrayList<>();
            
            // 1. Get candidates created directly by admin (User table with ROLE_PARTICIPANT)
            List<User> userCandidates = userRepository.findByUserType(UserType.ROLE_PARTICIPANT);
            for (User user : userCandidates) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", user.getId());
                row.put("userId", user.getId());
                row.put("name", user.getFullName());
                row.put("email", user.getEmail());
                row.put("party", user.getPartyName() != null ? user.getPartyName() : "Independent");
                row.put("phone", user.getPhoneNumber());
                row.put("status", user.getIsVerified() ? "APPROVED" : "PENDING");
                row.put("isVerified", user.getIsVerified());
                row.put("submittedAt", user.getCreatedAt());
                row.put("source", "admin_created");
                result.add(row);
            }
            
            // 2. Get candidate applications from Candidate table (user applications)
            List<Candidate> applications = candidateRepository.findAll();
            for (Candidate app : applications) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", app.getId());
                row.put("userId", app.getUser() != null ? app.getUser().getId() : null);
                row.put("name", app.getUser() != null ? app.getUser().getFullName() : "");
                row.put("email", app.getUser() != null ? app.getUser().getEmail() : "");
                row.put("party", app.getPartyName());
                row.put("electionId", app.getElection() != null ? app.getElection().getId() : null);
                row.put("electionTitle", app.getElection() != null ? app.getElection().getTitle() : "");
                row.put("status", app.getStatus() != null ? app.getStatus().name() : "PENDING");
                row.put("submittedAt", app.getCreatedAt());
                row.put("source", "user_application");
                result.add(row);
            }

            logger.debug("Retrieved {} admin-created candidates and {} user applications", userCandidates.size(), applications.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error fetching candidates", e);
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }
    
    // Add a new candidate
    @PostMapping("/candidates")
    public ResponseEntity<Map<String, Object>> addCandidate(@RequestBody Map<String, Object> candidateData) {
        logger.info("Processing candidate registration request");
        
        try {
            // Check if email already exists
            String email = (String) candidateData.get("email");
            if (email != null && userRepository.findByEmail(email).isPresent()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Email already exists");
                errorResponse.put("message", "A user with this email address already exists. Please use a different email.");
                return ResponseEntity.status(409).body(errorResponse); // 409 Conflict
            }
            
            // Create new candidate user  
            User candidate = new User();
            candidate.setFullName((String) candidateData.get("name"));
            candidate.setEmail((String) candidateData.get("email"));
            candidate.setPhoneNumber((String) candidateData.get("phone"));
            candidate.setPartyName((String) candidateData.get("party"));
            candidate.setUserType(UserType.ROLE_PARTICIPANT); // Temporary: using PARTICIPANT for candidates
            candidate.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD)); // Default password
            candidate.setAge(DEFAULT_AGE); // Default age
            candidate.setIsActive(true);
            candidate.setIsVerified(false);
            candidate.setCreatedAt(LocalDateTime.now());
            candidate.setUpdatedAt(LocalDateTime.now());
            
            // Save to database
            User savedCandidate = userRepository.save(candidate);
            
            // Create response
            Map<String, Object> result = new HashMap<>();
            result.put("id", savedCandidate.getId());
            result.put("name", savedCandidate.getFullName());
            result.put("party", savedCandidate.getPartyName());
            result.put("email", savedCandidate.getEmail());
            result.put("phone", savedCandidate.getPhoneNumber());
            result.put("status", "Pending");
            result.put("submittedAt", savedCandidate.getCreatedAt().toString());
            
            logger.info("Successfully created candidate with ID: {}", savedCandidate.getId());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error creating candidate: {}", e.getMessage());
            e.printStackTrace();
            
            // Return error response
            Map<String, Object> errorResponse = new HashMap<>();
            
            // Check if it's a duplicate email error
            if (e.getMessage().contains("Duplicate entry") && e.getMessage().contains("email")) {
                errorResponse.put("error", "Email already exists");
                errorResponse.put("message", "A user with this email address already exists. Please use a different email.");
                return ResponseEntity.status(409).body(errorResponse); // 409 Conflict
            } else {
                errorResponse.put("error", "Failed to create candidate");
                errorResponse.put("message", e.getMessage());
                return ResponseEntity.status(500).body(errorResponse);
            }
        }
    }
    
    // Update existing candidate
    @PutMapping("/candidates/{id}")
    public ResponseEntity<Map<String, Object>> updateCandidate(@PathVariable Long id, @RequestBody Map<String, Object> candidateData) {
        logger.info("Updating candidate with ID: {}", id);
        
        try {
            User candidate = userRepository.findById(id).orElse(null);
            if (candidate == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "Candidate not found");
                return ResponseEntity.notFound().build();
            }
            
            // Update candidate fields
            if (candidateData.containsKey("name")) {
                candidate.setFullName((String) candidateData.get("name"));
            }
            if (candidateData.containsKey("email")) {
                candidate.setEmail((String) candidateData.get("email"));
            }
            if (candidateData.containsKey("phone")) {
                candidate.setPhoneNumber((String) candidateData.get("phone"));
            }
            if (candidateData.containsKey("party")) {
                candidate.setPartyName((String) candidateData.get("party"));
            }
            
            candidate.setUpdatedAt(LocalDateTime.now());
            
            // Save to database
            User updatedCandidate = userRepository.save(candidate);
            
            // Create response
            Map<String, Object> result = new HashMap<>();
            result.put("id", updatedCandidate.getId());
            result.put("name", updatedCandidate.getFullName());
            result.put("party", updatedCandidate.getPartyName());
            result.put("email", updatedCandidate.getEmail());
            result.put("phone", updatedCandidate.getPhoneNumber());
            result.put("status", "Updated");
            result.put("updatedAt", updatedCandidate.getUpdatedAt().toString());
            
            logger.info("Successfully updated candidate: {}", updatedCandidate.getFullName());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error updating candidate: {}", e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to update candidate");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    // Delete candidate
    @DeleteMapping("/candidates/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteCandidate(@PathVariable Long id) {
        try {
            // Try Candidate table first
            Optional<Candidate> candidateOpt = candidateRepository.findById(id);
            if (candidateOpt.isPresent()) {
                candidateRepository.deleteById(id);
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Candidate deleted successfully");
                return ResponseEntity.ok(response);
            }
            
            // Fallback to User table
            if (userRepository.existsById(id)) {
                userRepository.deleteById(id);
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Candidate deleted successfully");
                return ResponseEntity.ok(response);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Candidate not found");
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error deleting candidate: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to delete candidate: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    // Upload symbol for a candidate
    @PostMapping("/candidates/{id}/symbol")
    public ResponseEntity<Map<String, Object>> uploadCandidateSymbol(
            @PathVariable("id") Long candidateId, 
            @RequestParam(value = "file", required = false) MultipartFile file) {
        
        if (file == null || file.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "No file provided or file is empty");
            errorResponse.put("message", "Please select a valid image file");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        logger.info("Processing symbol upload for candidate ID: {}", candidateId);
        
        try {
            // In a real app, this would upload the file and save the path to the database
            Map<String, Object> result = new HashMap<>();
            result.put("id", candidateId);
            result.put("symbolUrl", "uploaded-symbol-" + candidateId + ".png");
            result.put("status", "Symbol uploaded successfully");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to upload symbol");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    // Approve or reject a candidate
    @PostMapping("/candidates/{id}/status")
    public ResponseEntity<Map<String, Object>> updateCandidateStatus(
            @PathVariable("id") Long candidateId,
            @RequestBody Map<String, String> statusUpdate) {
        
        String newStatus = statusUpdate.get("status");
        logger.info("Updating status for candidate ID: {} to status: {}", candidateId, newStatus);
        
        try {
            // Try to find in Candidate table first (new format)
            Optional<Candidate> candidateOpt = candidateRepository.findById(candidateId);
            if (candidateOpt.isPresent()) {
                Candidate candidate = candidateOpt.get();
                
                if ("APPROVED".equals(newStatus)) {
                    candidate.setStatus(Candidate.CandidateStatus.APPROVED);
                    
                    // Auto-assign to election when approved
                    if (!electionParticipantRepository.existsByElectionIdAndCandidateId(
                            candidate.getElection().getId(), candidate.getId())) {
                        ElectionParticipant participant = new ElectionParticipant();
                        participant.setElection(candidate.getElection());
                        participant.setCandidate(candidate);
                        electionParticipantRepository.save(participant);
                        logger.info("Auto-assigned candidate {} to election {}", 
                                candidate.getUser().getFullName(), candidate.getElection().getTitle());
                    }
                } else if ("REJECTED".equals(newStatus)) {
                    candidate.setStatus(Candidate.CandidateStatus.REJECTED);
                } else {
                    candidate.setStatus(Candidate.CandidateStatus.PENDING);
                }
                
                candidate.setUpdatedAt(LocalDateTime.now());
                Candidate updatedCandidate = candidateRepository.save(candidate);
                
                Map<String, Object> result = new HashMap<>();
                result.put("id", updatedCandidate.getId());
                result.put("name", updatedCandidate.getUser().getFullName());
                result.put("status", updatedCandidate.getStatus().toString());
                result.put("updatedAt", updatedCandidate.getUpdatedAt().toString());
                
                return ResponseEntity.ok(result);
            }
            
            // Fallback to User table (old format)
            Optional<User> userOpt = userRepository.findById(candidateId);
            if (!userOpt.isPresent()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Candidate not found");
                return ResponseEntity.status(404).body(errorResponse);
            }
            
            User user = userOpt.get();
            user.setIsVerified("APPROVED".equals(newStatus));
            user.setUpdatedAt(LocalDateTime.now());
            User updatedUser = userRepository.save(user);
            
            Map<String, Object> result = new HashMap<>();
            result.put("id", updatedUser.getId());
            result.put("name", updatedUser.getFullName());
            result.put("status", updatedUser.getIsVerified() ? "APPROVED" : "PENDING");
            result.put("updatedAt", updatedUser.getUpdatedAt().toString());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error updating candidate status: {}", e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to update candidate status");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    // Elections endpoints
    @GetMapping("/elections")
    public ResponseEntity<List<Map<String, Object>>> getElections() {
        try {
            logger.debug("Fetching elections from database");
            // First try to get active elections, if none found, get all elections
            List<Election> elections = electionRepository.findByIsActiveTrueOrderByCreatedAtDesc();
            if (elections.isEmpty()) {
                logger.debug("No active elections found, fetching all elections");
                elections = electionRepository.findAll();
            }
            logger.debug("Retrieved {} elections from database", elections.size());
            List<Map<String, Object>> electionsList = new ArrayList<>();
            
            for (Election election : elections) {
                Map<String, Object> electionMap = new HashMap<>();
                electionMap.put("id", election.getId());
                electionMap.put("title", election.getTitle());
                electionMap.put("description", election.getDescription());
                electionMap.put("startDate", election.getStartDate() != null ? election.getStartDate().toString() : null);
                electionMap.put("endDate", election.getEndDate() != null ? election.getEndDate().toString() : null);
                electionMap.put("status", election.getStatus() != null ? election.getStatus().toString() : "DRAFT");
                // Attach participants from Candidate table (APPROVED) to avoid EP table dependency
                List<Long> participantIds;
                try {
                    List<Candidate> approved = candidateRepository.findByElectionIdAndStatus(
                            election.getId(), Candidate.CandidateStatus.APPROVED);
                    participantIds = approved.stream()
                            .map(Candidate::getId)
                            .toList();
                } catch (Exception ex) {
                    logger.warn("Failed to load approved candidates for election {}", election.getId());
                    participantIds = new ArrayList<>();
                }
                electionMap.put("participants", participantIds);
                electionMap.put("candidateCount", participantIds.size());
                electionMap.put("createdAt", election.getCreatedAt() != null ? election.getCreatedAt().toString() : null);
                electionMap.put("isActive", election.getIsActive());
                // Frontend will pass voterId via header for vote-status enrichment; leave false by default
                electionMap.put("hasVoted", false);
                electionsList.add(electionMap);
            }
            
            return ResponseEntity.ok(electionsList);
        } catch (Exception e) {
            logger.error("Error fetching elections", e);
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }
    
    @PostMapping("/elections")
    public ResponseEntity<Map<String, Object>> createElection(@RequestBody Map<String, Object> electionData) {
        logger.debug("Processing election creation request");
        
        try {
            // Create new election
            Election election = new Election();
            election.setTitle((String) electionData.get("title"));
            election.setDescription((String) electionData.get("description"));
            
            // Parse dates - assuming they come in ISO format
            String startDateStr = (String) electionData.get("startDate");
            String endDateStr = (String) electionData.get("endDate");
            
            if (startDateStr != null && !startDateStr.isEmpty()) {
                try {
                    // Handle ISO format with timezone (e.g., "2025-11-27T10:30:00+05:30" or "2025-11-27T10:30:00Z")
                    if (startDateStr.contains("T")) {
                        // Remove timezone offset if present and parse
                        String dateWithoutTz = startDateStr;
                        if (startDateStr.contains("+") || startDateStr.endsWith("Z")) {
                            // Extract date-time part before timezone
                            int tzIndex = startDateStr.lastIndexOf("+");
                            if (tzIndex == -1) tzIndex = startDateStr.lastIndexOf("-");
                            if (tzIndex > 10) { // Only if it's a timezone separator, not part of date
                                dateWithoutTz = startDateStr.substring(0, tzIndex);
                            } else if (startDateStr.endsWith("Z")) {
                                dateWithoutTz = startDateStr.substring(0, startDateStr.length() - 1);
                            }
                        }
                        election.setStartDate(LocalDateTime.parse(dateWithoutTz));
                    } else {
                        // If it's just a date, add time
                        election.setStartDate(LocalDateTime.parse(startDateStr + "T00:00:00"));
                    }
                } catch (Exception e) {
                    logger.error("Error parsing startDate: {}", startDateStr);
                    throw new RuntimeException("Invalid start date format: " + startDateStr, e);
                }
            }
            
            if (endDateStr != null && !endDateStr.isEmpty()) {
                try {
                    // Handle ISO format with timezone
                    if (endDateStr.contains("T")) {
                        // Remove timezone offset if present and parse
                        String dateWithoutTz = endDateStr;
                        if (endDateStr.contains("+") || endDateStr.endsWith("Z")) {
                            int tzIndex = endDateStr.lastIndexOf("+");
                            if (tzIndex == -1) tzIndex = endDateStr.lastIndexOf("-");
                            if (tzIndex > 10) {
                                dateWithoutTz = endDateStr.substring(0, tzIndex);
                            } else if (endDateStr.endsWith("Z")) {
                                dateWithoutTz = endDateStr.substring(0, endDateStr.length() - 1);
                            }
                        }
                        election.setEndDate(LocalDateTime.parse(dateWithoutTz));
                    } else {
                        election.setEndDate(LocalDateTime.parse(endDateStr + "T23:59:59"));
                    }
                } catch (Exception e) {
                    logger.error("Error parsing endDate: {}", endDateStr);
                    throw new RuntimeException("Invalid end date format: " + endDateStr, e);
                }
            }
            
            // Parse status from frontend
            String statusStr = (String) electionData.get("status");
            if (statusStr != null) {
                try {
                    election.setStatus(Election.ElectionStatus.valueOf(statusStr));
                } catch (IllegalArgumentException e) {
                    election.setStatus(Election.ElectionStatus.DRAFT);
                }
            } else {
                election.setStatus(Election.ElectionStatus.DRAFT);
            }
            election.setType(Election.ElectionType.OTHER);
            election.setIsActive(true);
            election.setMinAge(18);
            election.setAllowMultipleVotes(false);
            election.setCreatedAt(LocalDateTime.now());
            election.setUpdatedAt(LocalDateTime.now());
            
            // Save to database
            Election savedElection = electionRepository.save(election);
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("id", savedElection.getId());
            response.put("title", savedElection.getTitle());
            response.put("description", savedElection.getDescription());
            response.put("startDate", savedElection.getStartDate().toString());
            response.put("endDate", savedElection.getEndDate().toString());
            response.put("status", savedElection.getStatus().toString());
            response.put("candidateCount", 0);
            response.put("createdAt", savedElection.getCreatedAt().toString());
            
            System.out.println("Created election in database with ID: " + savedElection.getId());
            return ResponseEntity.status(201).body(response);
        } catch (Exception e) {
            System.err.println("Error creating election: " + e.getMessage());
            e.printStackTrace();
            
            // Return error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to create election");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    @PutMapping("/elections/{id}")
    public ResponseEntity<Map<String, Object>> updateElection(@PathVariable Long id, @RequestBody Map<String, Object> electionData) {
        System.out.println("Updating election with ID: " + id + ", data: " + electionData);
        
        try {
            Election election = electionRepository.findById(id).orElse(null);
            if (election == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Election not found");
                return ResponseEntity.notFound().build();
            }
            
            // Update election fields
            if (electionData.containsKey("title")) {
                election.setTitle((String) electionData.get("title"));
            }
            if (electionData.containsKey("description")) {
                election.setDescription((String) electionData.get("description"));
            }
            
            // Update dates
            if (electionData.containsKey("startDate")) {
                String startDateStr = (String) electionData.get("startDate");
                if (startDateStr != null && !startDateStr.isEmpty()) {
                    if (startDateStr.contains("T")) {
                        election.setStartDate(LocalDateTime.parse(startDateStr));
                    } else {
                        election.setStartDate(LocalDateTime.parse(startDateStr + "T00:00:00"));
                    }
                }
            }
            
            if (electionData.containsKey("endDate")) {
                String endDateStr = (String) electionData.get("endDate");
                if (endDateStr != null && !endDateStr.isEmpty()) {
                    if (endDateStr.contains("T")) {
                        election.setEndDate(LocalDateTime.parse(endDateStr));
                    } else {
                        election.setEndDate(LocalDateTime.parse(endDateStr + "T23:59:59"));
                    }
                }
            }
            
            // Update status
            if (electionData.containsKey("status")) {
                String statusStr = (String) electionData.get("status");
                if (statusStr != null) {
                    try {
                        election.setStatus(Election.ElectionStatus.valueOf(statusStr));
                    } catch (IllegalArgumentException e) {
                        // Keep existing status if invalid
                    }
                }
            }
            
            election.setUpdatedAt(LocalDateTime.now());
            
            // Save to database
            Election updatedElection = electionRepository.save(election);
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("id", updatedElection.getId());
            response.put("title", updatedElection.getTitle());
            response.put("description", updatedElection.getDescription());
            response.put("startDate", updatedElection.getStartDate().toString());
            response.put("endDate", updatedElection.getEndDate().toString());
            response.put("status", updatedElection.getStatus().toString());
            response.put("candidateCount", 0);
            response.put("updatedAt", updatedElection.getUpdatedAt().toString());
            
            System.out.println("Updated election in database: " + updatedElection.getTitle());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error updating election: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to update election");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    @DeleteMapping("/elections/{id}")
    public ResponseEntity<Map<String, String>> deleteElection(@PathVariable Long id) {
        try {
            if (electionRepository.existsById(id)) {
                electionRepository.deleteById(id);
                Map<String, String> response = new HashMap<>();
                response.put("message", "Election deleted successfully");
                response.put("deletedId", id.toString());
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to delete election: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    @PostMapping("/elections/{id}/start")
    public ResponseEntity<Map<String, Object>> startElection(@PathVariable Long id) {
        try {
            Election election = electionRepository.findById(id).orElse(null);
            if (election == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Election not found");
                return ResponseEntity.notFound().build();
            }
            
            election.setStatus(Election.ElectionStatus.ACTIVE);
            election.setUpdatedAt(LocalDateTime.now());
            electionRepository.save(election);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", id);
            response.put("status", "ACTIVE");
            response.put("startedAt", LocalDateTime.now().toString());
            response.put("message", "Election started successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error starting election: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to start election");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    @PostMapping("/elections/{id}/end")
    public ResponseEntity<Map<String, Object>> endElection(@PathVariable Long id) {
        try {
            Election election = electionRepository.findById(id).orElse(null);
            if (election == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Election not found");
                return ResponseEntity.notFound().build();
            }
            
            election.setStatus(Election.ElectionStatus.COMPLETED);
            election.setUpdatedAt(LocalDateTime.now());
            electionRepository.save(election);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", id);
            response.put("status", "COMPLETED");
            response.put("endedAt", LocalDateTime.now().toString());
            response.put("message", "Election ended successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error ending election: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to end election");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    @PostMapping("/elections/{id}/schedule")
    public ResponseEntity<Map<String, Object>> scheduleElection(@PathVariable Long id, @RequestBody Map<String, Object> scheduleData) {
        // In a real application, this would update the election schedule in database
        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("startDate", scheduleData.get("startDate"));
        response.put("endDate", scheduleData.get("endDate"));
        response.put("status", "SCHEDULED");
        response.put("scheduledAt", java.time.LocalDateTime.now().toString());
        response.put("message", "Election scheduled successfully");
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/elections/{id}/candidates")
    public ResponseEntity<List<Map<String, Object>>> getElectionCandidates(@PathVariable Long id) {
        // In a real application, this would fetch candidates for an election from database
        List<Map<String, Object>> candidates = new ArrayList<>();
        if (id == 1L) {
            candidates.add(createMockCandidate(1L, "Jane Smith", "Independent", "Approved", null));
            candidates.add(createMockCandidate(2L, "Mike Brown", "Party A", "Approved", "party-a-symbol.png"));
        } else if (id == 2L) {
            candidates.add(createMockCandidate(3L, "Sarah Davis", "Party B", "Approved", null));
            candidates.add(createMockCandidate(4L, "Robert Johnson", "Party C", "Approved", "party-c-symbol.png"));
        }
        return ResponseEntity.ok(candidates);
    }
    
    @PostMapping("/elections/candidates/assign")
    public ResponseEntity<Map<String, Object>> assignCandidatesToElection(@RequestBody Map<String, Object> assignmentData) {
        try {
            Long electionId = Long.valueOf(assignmentData.get("electionId").toString());
            @SuppressWarnings("unchecked")
            List<Long> candidateIds = (List<Long>) assignmentData.get("candidateIds");
            
            Optional<Election> electionOpt = electionRepository.findById(electionId);
            if (!electionOpt.isPresent()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Election not found");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            Election election = electionOpt.get();
            int assignedCount = 0;
            
            for (Long candidateId : candidateIds) {
                Optional<Candidate> candidateOpt = candidateRepository.findById(candidateId);
                if (candidateOpt.isPresent() && 
                    !electionParticipantRepository.existsByElectionIdAndCandidateId(electionId, candidateId)) {
                    
                    ElectionParticipant participant = new ElectionParticipant();
                    participant.setElection(election);
                    participant.setCandidate(candidateOpt.get());
                    electionParticipantRepository.save(participant);
                    assignedCount++;
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("electionId", electionId);
            response.put("assignedCount", assignedCount);
            response.put("assignedAt", LocalDateTime.now().toString());
            response.put("message", assignedCount + " candidates assigned successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to assign candidates");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    // System settings
    @GetMapping("/settings/system-config")
    public ResponseEntity<Map<String, Object>> getSystemConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("timezone", "UTC");
        config.put("maxVotingHours", 24);
        config.put("voterVerificationEnabled", true);
        config.put("auditLoggingEnabled", true);
        return ResponseEntity.ok(config);
    }
    
    // Audit logs
    @GetMapping("/audit-logs")
    public ResponseEntity<List<Map<String, Object>>> getAuditLogs() {
        List<Map<String, Object>> logs = new ArrayList<>();
        logs.add(createMockAuditLog(1L, "LOGIN", "admin@voterow.com", "2025-09-21T10:30:00"));
        logs.add(createMockAuditLog(2L, "CREATE_VOTER", "admin@voterow.com", "2025-09-21T09:15:00"));
        logs.add(createMockAuditLog(3L, "START_ELECTION", "admin@voterow.com", "2025-09-21T08:00:00"));
        logs.add(createMockAuditLog(4L, "APPROVE_CANDIDATE", "admin@voterow.com", "2025-09-21T07:45:00"));
        return ResponseEntity.ok(logs);
    }
    
    // Monitor live voting
    @GetMapping("/elections/{id}/monitor")
    public ResponseEntity<Map<String, Object>> monitorElection(@PathVariable Long id) {
        try {
            Map<String, Object> monitor = new HashMap<>();
            monitor.put("electionId", id);
            monitor.put("totalVotes", 0); // TODO: implement actual vote counting
            monitor.put("activeVoters", 0);
            monitor.put("lastVoteTime", LocalDateTime.now());
            monitor.put("status", "ACTIVE");
            return ResponseEntity.ok(monitor);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to get monitoring data"));
        }
    }
    
    // Get election results
    @GetMapping("/elections/{id}/results")
    public ResponseEntity<Map<String, Object>> getElectionResults(@PathVariable Long id) {
        try {
            Map<String, Object> results = new HashMap<>();
            results.put("electionId", id);
            results.put("totalVotes", 0);
            results.put("candidates", new ArrayList<>());
            results.put("winner", null);
            results.put("publishedAt", LocalDateTime.now());
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to get results"));
        }
    }
    
    // Send voter credentials
    @PostMapping("/voters/{id}/send-credentials")
    public ResponseEntity<Map<String, Object>> sendVoterCredentials(@PathVariable Long id) {
        try {
            Optional<User> userOpt = userRepository.findById(id);
            if (!userOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            User voter = userOpt.get();
            // TODO: Implement actual email/SMS sending
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Credentials sent to " + voter.getEmail());
            response.put("sentAt", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to send credentials"));
        }
    }
    
    // System health check endpoint
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        try {
            // Check database connectivity
            long userCount = userRepository.count();
            long electionCount = electionRepository.count();
            long candidateCount = candidateRepository.count();
            
            status.put("status", "HEALTHY");
            status.put("version", "1.0.0");
            status.put("database", "CONNECTED");
            status.put("userCount", userCount);
            status.put("electionCount", electionCount);
            status.put("candidateCount", candidateCount);
            status.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            status.put("status", "UNHEALTHY");
            status.put("error", e.getMessage());
            status.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(500).body(status);
        }
    }
    
    // Helper methods to create mock data
    private Map<String, Object> createMockUser(Long id, String name, String email, String userType, boolean isActive, boolean isVerified) {
        Map<String, Object> user = new HashMap<>();
        user.put("id", id);
        user.put("fullName", name);
        user.put("email", email);
        user.put("userType", userType);
        user.put("isActive", isActive);
        user.put("isVerified", isVerified);
        user.put("age", 25 + (id.intValue() * 5));
        return user;
    }
    
    private Map<String, Object> createMockCandidate(Long id, String name, String party, String status, String symbolUrl) {
        Map<String, Object> candidate = new HashMap<>();
        candidate.put("id", id);
        candidate.put("name", name);
        candidate.put("party", party);
        candidate.put("status", status);
        candidate.put("symbolUrl", symbolUrl);
        candidate.put("election", Map.of("title", "Presidential Election 2025"));
        return candidate;
    }
    
    private Map<String, Object> createMockElection(Long id, String title, String startDate, String endDate, String status, int candidateCount) {
        Map<String, Object> election = new HashMap<>();
        election.put("id", id);
        election.put("title", title);
        election.put("startDate", startDate);
        election.put("endDate", endDate);
        election.put("status", status);
        election.put("candidateCount", candidateCount);
        return election;
    }
    
    // ========== ELECTION MANAGEMENT ENDPOINTS ==========
    
    /**
     * Assign candidates to election as participants
     */
    @PostMapping("/elections/{electionId}/assign-candidates")
    public ResponseEntity<Map<String, Object>> assignCandidatesToElection(
            @PathVariable Long electionId,
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Integer> candidateIds = (List<Integer>) request.get("candidateIds");
            
            Optional<Election> electionOpt = electionRepository.findById(electionId);
            if (!electionOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "Election not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            Election election = electionOpt.get();
            List<String> assignedCandidates = new ArrayList<>();
            
            for (Integer candidateId : candidateIds) {
                Optional<Candidate> candidateOpt = candidateRepository.findById(candidateId.longValue());
                if (candidateOpt.isPresent() && candidateOpt.get().getStatus() == Candidate.CandidateStatus.APPROVED) {
                    
                    // Check if already assigned
                    boolean alreadyAssigned = electionParticipantRepository
                        .existsByElectionAndCandidate(election, candidateOpt.get());
                        
                    if (!alreadyAssigned) {
                        ElectionParticipant participant = new ElectionParticipant();
                        participant.setElection(election);
                        participant.setCandidate(candidateOpt.get());
                        participant.setAssignedAt(LocalDateTime.now());
                        participant.setIsActive(true);
                        
                        electionParticipantRepository.save(participant);
                        assignedCandidates.add(candidateOpt.get().getUser().getFullName());
                    }
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Candidates assigned to election successfully");
            response.put("assignedCandidates", assignedCandidates);
            response.put("electionTitle", election.getTitle());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error assigning candidates to election", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to assign candidates to election");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Get election participants (assigned candidates)
     */
    @GetMapping("/elections/{electionId}/participants")
    public ResponseEntity<List<Map<String, Object>>> getElectionParticipants(@PathVariable Long electionId) {
        try {
            Optional<Election> electionOpt = electionRepository.findById(electionId);
            if (!electionOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            List<ElectionParticipant> participants = electionParticipantRepository
                .findByElectionAndIsActiveTrue(electionOpt.get());
            
            List<Map<String, Object>> participantData = new ArrayList<>();
            for (ElectionParticipant participant : participants) {
                Map<String, Object> data = new HashMap<>();
                data.put("id", participant.getId());
                data.put("candidateId", participant.getCandidate().getId());
                data.put("candidateName", participant.getCandidate().getUser().getFullName());
                data.put("partyName", participant.getCandidate().getPartyName());
                data.put("assignedAt", participant.getAssignedAt().toString());
                data.put("isActive", participant.getIsActive());
                participantData.add(data);
            }
            
            return ResponseEntity.ok(participantData);
            
        } catch (Exception e) {
            logger.error("Error fetching election participants", e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * Remove candidate from election
     */
    @DeleteMapping("/elections/{electionId}/participants/{participantId}")
    public ResponseEntity<Map<String, Object>> removeParticipant(
            @PathVariable Long electionId, 
            @PathVariable Long participantId) {
        try {
            Optional<ElectionParticipant> participantOpt = electionParticipantRepository.findById(participantId);
            if (!participantOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "Participant not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            ElectionParticipant participant = participantOpt.get();
            participant.setIsActive(false);
            electionParticipantRepository.save(participant);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Candidate removed from election successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error removing participant from election", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to remove participant");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    private Map<String, Object> createMockAuditLog(Long id, String action, String user, String timestamp) {
        Map<String, Object> log = new HashMap<>();
        log.put("id", id);
        log.put("action", action);
        log.put("user", user);
        log.put("timestamp", timestamp);
        return log;
    }
    
    // ========== ELECTION WORKFLOW ENDPOINTS ==========
    
    @PostMapping("/elections/{id}/open")
    public ResponseEntity<Map<String, Object>> openElectionForVoting(@PathVariable Long id) {
        try {
            User admin = getCurrentAdmin(); // Implementation needed
            Election election = electionWorkflowService.openElection(id, admin);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Election opened for voting");
            response.put("electionId", election.getId());
            response.put("status", election.getStatus().toString());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to open election: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ========== COMPLETE ELECTION WORKFLOW ENDPOINTS ==========
    
    /**
     * STEP 1: Publish Election (DRAFT -> RESULTS_PUBLISHED/PUBLISHED state)
     * Admin transitions an election from DRAFT to PUBLISHED state
     */
    @PostMapping("/elections/{id}/publish")
    public ResponseEntity<Map<String, Object>> publishElection(@PathVariable Long id) {
        try {
            User admin = getCurrentAdmin();
            if (admin == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            
            Election election = electionWorkflowService.publishElection(id, admin);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Election published successfully");
            response.put("electionId", election.getId());
            response.put("electionTitle", election.getTitle());
            response.put("status", election.getStatus().toString());
            response.put("publishedAt", LocalDateTime.now().toString());
            
            System.out.println("Election " + election.getId() + " published by admin");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error publishing election: " + e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to publish election");
            response.put("message", e.getMessage());
            return ResponseEntity.status(400).body(response);
        }
    }

    /**
     * STEP 2: Start/Open Election for Voting (RESULTS_PUBLISHED/PUBLISHED -> ACTIVE)
     * Admin opens the published election for voting
     */
    @PostMapping("/elections/{id}/activate")
    public ResponseEntity<Map<String, Object>> activateElectionForVoting(@PathVariable Long id) {
        try {
            User admin = getCurrentAdmin();
            if (admin == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            
            Election election = electionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Election not found"));
            
            // Validate election can be activated
            if (election.getStatus() != Election.ElectionStatus.RESULTS_PUBLISHED && 
                election.getStatus() != Election.ElectionStatus.DRAFT) {
                throw new RuntimeException("Election must be in PUBLISHED or DRAFT state to activate. Current: " + election.getStatus());
            }
            
            // Set to ACTIVE
            election.setStatus(Election.ElectionStatus.ACTIVE);
            election.setUpdatedAt(LocalDateTime.now());
            Election savedElection = electionRepository.save(election);
            
            auditLogService.logAction(admin, "ACTIVATE_ELECTION", "Election", id, 
                "Election activated for voting: " + election.getTitle());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Election activated for voting");
            response.put("electionId", savedElection.getId());
            response.put("status", "ACTIVE");
            response.put("activatedAt", LocalDateTime.now().toString());
            
            System.out.println("Election " + id + " activated for voting");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error activating election: " + e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to activate election");
            response.put("message", e.getMessage());
            return ResponseEntity.status(400).body(response);
        }
    }

    /**
     * STEP 3: Close Election (ACTIVE -> COMPLETED)
     * Admin closes the election after voting period ends
     */
    @PostMapping("/elections/{id}/close")
    public ResponseEntity<Map<String, Object>> closeElection(@PathVariable Long id) {
        try {
            User admin = getCurrentAdmin();
            if (admin == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            
            Election election = electionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Election not found"));
            
            if (election.getStatus() != Election.ElectionStatus.ACTIVE) {
                throw new RuntimeException("Only ACTIVE elections can be closed. Current: " + election.getStatus());
            }
            
            election.setStatus(Election.ElectionStatus.COMPLETED);
            election.setUpdatedAt(LocalDateTime.now());
            Election savedElection = electionRepository.save(election);
            
            auditLogService.logAction(admin, "CLOSE_ELECTION", "Election", id, 
                "Election closed: " + election.getTitle());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Election closed successfully");
            response.put("electionId", savedElection.getId());
            response.put("status", "COMPLETED");
            response.put("closedAt", LocalDateTime.now().toString());
            
            System.out.println("Election " + id + " closed");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error closing election: " + e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to close election");
            response.put("message", e.getMessage());
            return ResponseEntity.status(400).body(response);
        }
    }

    /**
     * STEP 4: Calculate and Get Election Results
     * Calculate vote tallies after election is closed
     */
    @GetMapping("/elections/{id}/results/calculate")
    public ResponseEntity<Map<String, Object>> calculateResults(@PathVariable Long id) {
        try {
            User admin = getCurrentAdmin();
            if (admin == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            
            Map<String, Object> results = electionWorkflowService.calculateElectionResults(id);
            
            auditLogService.logAction(admin, "CALCULATE_RESULTS", "Election", id, 
                "Results calculated for election");
            
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            System.err.println("Error calculating results: " + e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to calculate results");
            response.put("message", e.getMessage());
            return ResponseEntity.status(400).body(response);
        }
    }

    /**
     * STEP 5: Publish Results (COMPLETED -> RESULTS_PUBLISHED)
     * Admin publishes the calculated results
     */
    @PostMapping("/elections/{id}/publish-results")
    public ResponseEntity<Map<String, Object>> publishResults(@PathVariable Long id) {
        try {
            User admin = getCurrentAdmin();
            if (admin == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            
            Election election = electionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Election not found"));
            
            if (election.getStatus() != Election.ElectionStatus.COMPLETED) {
                throw new RuntimeException("Only COMPLETED elections can publish results. Current: " + election.getStatus());
            }
            
            // Set status to RESULTS_PUBLISHED
            election.setStatus(Election.ElectionStatus.RESULTS_PUBLISHED);
            election.setUpdatedAt(LocalDateTime.now());
            Election savedElection = electionRepository.save(election);
            
            // Notify all participants about results
            List<Candidate> candidates = candidateRepository.findByElectionIdAndStatus(id, Candidate.CandidateStatus.APPROVED);
            for (Candidate candidate : candidates) {
                if (candidate.getStatus() == Candidate.CandidateStatus.APPROVED) {
                    notificationService.sendElectionNotification(
                        candidate.getUser(),
                        "Election Results Published",
                        "Results for election '" + election.getTitle() + "' have been published",
                        "RESULTS_PUBLISHED"
                    );
                }
            }
            
            auditLogService.logAction(admin, "PUBLISH_RESULTS", "Election", id, 
                "Results published for election: " + election.getTitle());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Election results published successfully");
            response.put("electionId", savedElection.getId());
            response.put("status", "RESULTS_PUBLISHED");
            response.put("publishedAt", LocalDateTime.now().toString());
            
            System.out.println("Results published for election " + id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error publishing results: " + e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to publish results");
            response.put("message", e.getMessage());
            return ResponseEntity.status(400).body(response);
        }
    }

    /**
     * Get Eligible Voters for an Election
     * Lists all voters who meet the eligibility criteria
     */
    @GetMapping("/elections/{id}/eligible-voters")
    public ResponseEntity<Map<String, Object>> getEligibleVoters(@PathVariable Long id) {
        try {
            Election election = electionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Election not found"));
            
            List<User> eligibleVoters = electionWorkflowService.getEligibleVoters(id);
            
            List<Map<String, Object>> voterData = new ArrayList<>();
            for (User voter : eligibleVoters) {
                Map<String, Object> data = new HashMap<>();
                data.put("id", voter.getId());
                data.put("name", voter.getFullName());
                data.put("email", voter.getEmail());
                data.put("age", voter.getAge());
                data.put("isVerified", voter.getIsVerified());
                voterData.add(data);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("electionId", id);
            response.put("electionTitle", election.getTitle());
            response.put("totalEligibleVoters", voterData.size());
            response.put("voters", voterData);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error fetching eligible voters: " + e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to fetch eligible voters");
            response.put("message", e.getMessage());
            return ResponseEntity.status(400).body(response);
        }
    }

    /**
     * Get Vote Count for an Election (Real-time monitoring)
     */
    @GetMapping("/elections/{id}/vote-count")
    public ResponseEntity<Map<String, Object>> getVoteCount(@PathVariable Long id) {
        try {
            Election election = electionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Election not found"));
            
            List<Vote> allVotes = voteRepository.findByElection(election);
            
            Map<String, Object> response = new HashMap<>();
            response.put("electionId", id);
            response.put("electionTitle", election.getTitle());
            response.put("totalVotes", allVotes.size());
            response.put("electionStatus", election.getStatus().toString());
            response.put("lastUpdated", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error fetching vote count: " + e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to fetch vote count");
            response.put("message", e.getMessage());
            return ResponseEntity.status(400).body(response);
        }
    }


    @GetMapping("/elections/{id}/eligibility")
    public ResponseEntity<Map<String, Object>> checkVoterEligibility(@PathVariable Long id, @RequestParam Long voterId) {
        try {
            User voter = userRepository.findById(voterId).orElseThrow(() -> new RuntimeException("Voter not found"));
            Election election = electionRepository.findById(id).orElseThrow(() -> new RuntimeException("Election not found"));
            
            boolean eligible = electionWorkflowService.validateVoterEligibility(voter, election);
            
            Map<String, Object> response = new HashMap<>();
            response.put("eligible", eligible);
            response.put("voterId", voterId);
            response.put("electionId", id);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to check eligibility: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    private User getCurrentAdmin() {
        // Placeholder - implement proper admin user retrieval
        return userRepository.findByEmail("admin@voterow.com").orElse(null);
    }
}