package com.voterow.backend.controller;

import com.voterow.backend.model.User;
import com.voterow.backend.model.UserType;
import com.voterow.backend.model.Election;
import com.voterow.backend.repository.UserRepository;
import com.voterow.backend.repository.ElectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private static final String DEFAULT_PASSWORD = "temp123";
    private static final int DEFAULT_AGE = 25;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ElectionRepository electionRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
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
            System.err.println("Error fetching voters: " + e.getMessage());
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
        // In a real app, this would generate CSV from database records
        StringBuilder csv = new StringBuilder();
        csv.append("id,fullName,email,age,status,verified\n");
        csv.append("2,John Doe,john@example.com,32,Active,Yes\n");
        csv.append("4,Alice Johnson,alice@example.com,28,Active,Yes\n");
        csv.append("5,Bob Wilson,bob@example.com,45,Inactive,No\n");
        
        return ResponseEntity
            .ok()
            .header("Content-Type", "text/csv")
            .header("Content-Disposition", "attachment; filename=voters.csv")
            .body(csv.toString());
    }
    
    // Delete voter
    @DeleteMapping("/voters/{id}")
    public ResponseEntity<Map<String, Object>> deleteVoter(@PathVariable Long id) {
        try {
            if (userRepository.existsById(id)) {
                userRepository.deleteById(id);
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Voter deleted successfully");
                System.out.println("Deleted voter with ID: " + id);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Voter not found");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.err.println("Error deleting voter: " + e.getMessage());
            e.printStackTrace();
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
            
            System.out.println("Updated voter: " + user.getFullName());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("Error updating voter: " + e.getMessage());
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
            
            System.out.println("Toggled verification for voter: " + user.getFullName() + " to " + isVerified);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("Error toggling voter verification: " + e.getMessage());
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
            
            System.out.println("Toggled status for voter: " + user.getFullName() + " to " + (isActive ? "Active" : "Inactive"));
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("Error toggling voter status: " + e.getMessage());
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
            List<User> candidates = userRepository.findByUserType(UserType.ROLE_PARTICIPANT); // Temporary: using PARTICIPANT for candidates
            List<Map<String, Object>> candidatesList = new ArrayList<>();
            
            System.out.println("Found " + candidates.size() + " candidates in database");
            
            for (User candidate : candidates) {
                Map<String, Object> candidateMap = new HashMap<>();
                candidateMap.put("id", candidate.getId());
                candidateMap.put("name", candidate.getFullName());
                candidateMap.put("party", candidate.getPartyName() != null ? candidate.getPartyName() : "Independent");
                candidateMap.put("email", candidate.getEmail());
                candidateMap.put("phone", candidate.getPhoneNumber());
                
                // Use isVerified to determine candidate approval status
                String status;
                if (candidate.getIsVerified() == null || !candidate.getIsVerified()) {
                    status = "PENDING";
                } else {
                    status = "APPROVED";
                }
                candidateMap.put("status", status);
                candidateMap.put("appliedDate", candidate.getCreatedAt().toString());
                candidateMap.put("userId", candidate.getId()); // This is the user ID who applied as candidate
                candidateMap.put("electionId", 1); // Default election ID - in real app, this would come from candidate application data
                candidateMap.put("isActive", candidate.getIsActive());
                candidateMap.put("registeredAt", candidate.getCreatedAt());
                candidateMap.put("election", Map.of("title", "Presidential Election 2025")); // Mock election for now
                candidateMap.put("symbolUrl", ""); // Empty for now
                
                System.out.println("Candidate: " + candidate.getFullName() + " - Status: " + status + " (isVerified: " + candidate.getIsVerified() + ")");
                candidatesList.add(candidateMap);
            }
            
            System.out.println("Returning " + candidatesList.size() + " candidates to frontend");
            
            return ResponseEntity.ok(candidatesList);
        } catch (Exception e) {
            System.err.println("Error fetching candidates: " + e.getMessage());
            e.printStackTrace();
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
    public ResponseEntity<Map<String, Object>> deleteCandidate(@PathVariable Long id) {
        try {
            if (userRepository.existsById(id)) {
                userRepository.deleteById(id);
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Candidate deleted successfully");
                logger.info("Successfully deleted candidate with ID: {}", id);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Candidate not found");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error deleting candidate: {}", e.getMessage());
            e.printStackTrace();
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
        System.out.println("Updating status for candidate: " + candidateId + " to " + newStatus);
        
        try {
            // Find the candidate in database
            Optional<User> candidateOpt = userRepository.findById(candidateId);
            if (!candidateOpt.isPresent()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Candidate not found");
                errorResponse.put("message", "No candidate found with ID: " + candidateId);
                return ResponseEntity.notFound().body(errorResponse);
            }
            
            User candidate = candidateOpt.get();
            
            // Update the isVerified field based on status
            if ("APPROVED".equals(newStatus)) {
                candidate.setIsVerified(true);
                System.out.println("Approved candidate: " + candidate.getFullName());
            } else if ("PENDING".equals(newStatus) || "REJECTED".equals(newStatus)) {
                candidate.setIsVerified(false);
                System.out.println("Set candidate status to " + newStatus + ": " + candidate.getFullName());
            }
            
            candidate.setUpdatedAt(LocalDateTime.now());
            
            // Save to database
            User updatedCandidate = userRepository.save(candidate);
            
            // Return updated candidate info
            Map<String, Object> result = new HashMap<>();
            result.put("id", updatedCandidate.getId());
            result.put("name", updatedCandidate.getFullName());
            result.put("status", updatedCandidate.getIsVerified() ? "APPROVED" : "PENDING");
            result.put("isVerified", updatedCandidate.getIsVerified());
            result.put("updatedAt", updatedCandidate.getUpdatedAt().toString());
            
            System.out.println("Successfully updated candidate status in database");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("Error updating candidate status: " + e.getMessage());
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
            List<Election> elections = electionRepository.findByIsActiveTrueOrderByCreatedAtDesc();
            List<Map<String, Object>> electionsList = new ArrayList<>();
            
            for (Election election : elections) {
                Map<String, Object> electionMap = new HashMap<>();
                electionMap.put("id", election.getId());
                electionMap.put("title", election.getTitle());
                electionMap.put("description", election.getDescription());
                electionMap.put("startDate", election.getStartDate().toString());
                electionMap.put("endDate", election.getEndDate().toString());
                electionMap.put("status", election.getStatus().toString());
                electionMap.put("candidateCount", 0); // TODO: count actual candidates
                electionMap.put("createdAt", election.getCreatedAt().toString());
                electionMap.put("isActive", election.getIsActive());
                electionsList.add(electionMap);
            }
            
            return ResponseEntity.ok(electionsList);
        } catch (Exception e) {
            System.err.println("Error fetching elections: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }
    
    @PostMapping("/elections")
    public ResponseEntity<Map<String, Object>> createElection(@RequestBody Map<String, Object> electionData) {
        System.out.println("Received election data: " + electionData);
        
        try {
            // Create new election
            Election election = new Election();
            election.setTitle((String) electionData.get("title"));
            election.setDescription((String) electionData.get("description"));
            
            // Parse dates - assuming they come in ISO format
            String startDateStr = (String) electionData.get("startDate");
            String endDateStr = (String) electionData.get("endDate");
            
            if (startDateStr != null && !startDateStr.isEmpty()) {
                // If date includes 'T', it's already in datetime format
                if (startDateStr.contains("T")) {
                    election.setStartDate(LocalDateTime.parse(startDateStr));
                } else {
                    // If it's just a date, add time
                    election.setStartDate(LocalDateTime.parse(startDateStr + "T00:00:00"));
                }
            }
            
            if (endDateStr != null && !endDateStr.isEmpty()) {
                if (endDateStr.contains("T")) {
                    election.setEndDate(LocalDateTime.parse(endDateStr));
                } else {
                    election.setEndDate(LocalDateTime.parse(endDateStr + "T23:59:59"));
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
        // In a real application, this would delete from database
        Map<String, String> response = new HashMap<>();
        response.put("message", "Election deleted successfully");
        response.put("deletedId", id.toString());
        
        return ResponseEntity.ok(response);
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
        // In a real application, this would assign candidates to an election in database
        Map<String, Object> response = new HashMap<>();
        response.put("electionId", assignmentData.get("electionId"));
        response.put("candidateIds", assignmentData.get("candidateIds"));
        response.put("assignedAt", java.time.LocalDateTime.now().toString());
        response.put("message", "Candidates assigned successfully");
        
        return ResponseEntity.ok(response);
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
    
    // Basic status endpoint
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getStatus() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "RUNNING");
        status.put("version", "1.0.0");
        status.put("uptime", "2 hours");
        return ResponseEntity.ok(status);
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
    
    private Map<String, Object> createMockAuditLog(Long id, String action, String user, String timestamp) {
        Map<String, Object> log = new HashMap<>();
        log.put("id", id);
        log.put("action", action);
        log.put("user", user);
        log.put("timestamp", timestamp);
        return log;
    }
}