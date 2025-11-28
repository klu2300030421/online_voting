package com.voterow.backend.controller;

import com.voterow.backend.dto.CampaignMaterialRequest;
import com.voterow.backend.model.CampaignMaterial;
import com.voterow.backend.model.Candidate;
import com.voterow.backend.model.Election;
import com.voterow.backend.model.User;
import com.voterow.backend.repository.CandidateRepository;
import com.voterow.backend.repository.ElectionRepository;
import com.voterow.backend.repository.UserRepository;
import com.voterow.backend.service.CampaignMaterialService;
import com.voterow.backend.service.ElectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/participant")
@RequiredArgsConstructor
public class ParticipantController {

    private static final Logger logger = LoggerFactory.getLogger(ParticipantController.class);

    private final ElectionService electionService;
    private final UserRepository userRepository;
    private final CampaignMaterialService campaignMaterialService;
    private final CandidateRepository candidateRepository;
    private final ElectionRepository electionRepository;

    @GetMapping("/elections")
    public ResponseEntity<List<Map<String, Object>>> getAvailableElections() {
        try {
            logger.debug("Fetching ALL elections from database");
            List<Election> elections = electionRepository.findAll();
            logger.debug("Found {} elections in database", elections.size());
            List<Map<String, Object>> electionsList = new ArrayList<>();
            
            for (Election election : elections) {
                Map<String, Object> electionMap = new HashMap<>();
                electionMap.put("id", election.getId());
                electionMap.put("title", election.getTitle());
                electionMap.put("description", election.getDescription());
                electionMap.put("startDate", election.getStartDate() != null ? election.getStartDate().toString() : null);
                electionMap.put("endDate", election.getEndDate() != null ? election.getEndDate().toString() : null);
                electionMap.put("status", election.getStatus() != null ? election.getStatus().toString() : "DRAFT");
                electionMap.put("createdAt", election.getCreatedAt() != null ? election.getCreatedAt().toString() : null);
                electionMap.put("isActive", election.getIsActive());
                electionsList.add(electionMap);
            }
            
            logger.debug("Returning {} elections for participant", electionsList.size());
            return ResponseEntity.ok(electionsList);
        } catch (Exception e) {
            logger.error("Error fetching elections for participants", e);
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    @PostMapping("/elections/{id}/enroll")
    public ResponseEntity<Election> enrollInElection(@PathVariable Long id, Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName()).orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(electionService.enrollParticipant(id, currentUser));
    }
    
    // Candidate Application Endpoint
    @PostMapping("/candidate-application")
    public ResponseEntity<Map<String, Object>> submitCandidateApplication(@RequestBody Map<String, Object> applicationData) {
        logger.debug("Processing candidate application");
        
        try {
            // Extract application data with null checks
            Object userIdObj = applicationData.get("userId");
            Object electionIdObj = applicationData.get("electionId");
            String partyName = (String) applicationData.get("party");
            
            if (userIdObj == null || electionIdObj == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Missing userId or electionId");
                logger.warn("Missing required fields in application data");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            Long userId = Long.valueOf(userIdObj.toString());
            Long electionId = Long.valueOf(electionIdObj.toString());
            
            logger.debug("Processing application for userId={}, electionId={}", userId, electionId);
            
            // Find user and election
            Optional<User> userOpt = userRepository.findById(userId);
            Optional<Election> electionOpt = electionRepository.findById(electionId);
            
            if (!userOpt.isPresent()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "User not found with ID: " + userId);
                logger.debug("User not found: {}", userId);
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            if (!electionOpt.isPresent()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Election not found with ID: " + electionId);
                logger.debug("Election not found: {}", electionId);
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            User user = userOpt.get();
            Election election = electionOpt.get();
            
            logger.debug("Processing application for user: {}, election: {}", user.getFullName(), election.getTitle());
            
            // Check if user already applied for this election
            List<Candidate> existingApplications = candidateRepository.findByUserIdAndElectionId(userId, electionId);
            if (!existingApplications.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "You have already applied for this election");
                logger.debug("User already applied for this election");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Create new candidate application
            Candidate candidate = new Candidate();
            candidate.setUser(user);
            candidate.setElection(election);
            candidate.setPartyName(partyName != null ? partyName : "Independent");
            candidate.setStatus(Candidate.CandidateStatus.PENDING);
            candidate.setIsActive(true);
            candidate.setCreatedAt(LocalDateTime.now());
            candidate.setUpdatedAt(LocalDateTime.now());
            
            logger.debug("Saving candidate application");
            
            // Save candidate application
            Candidate savedCandidate = candidateRepository.save(candidate);
            
            logger.debug("Successfully saved candidate application with ID: {}", savedCandidate.getId());
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("id", savedCandidate.getId());
            response.put("userId", savedCandidate.getUser().getId());
            response.put("electionId", savedCandidate.getElection().getId());
            response.put("partyName", savedCandidate.getPartyName());
            response.put("status", savedCandidate.getStatus().toString());
            response.put("submittedAt", savedCandidate.getCreatedAt().toString());
            response.put("message", "Candidate application submitted successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (NumberFormatException e) {
            logger.error("Invalid number format in application data", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid user ID or election ID format");
            errorResponse.put("message", "Please ensure user ID and election ID are valid numbers");
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            logger.error("Error processing candidate application", e);
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to submit candidate application");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("details", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    // Campaign Materials Endpoints
    @GetMapping("/campaign-materials")
    public ResponseEntity<List<CampaignMaterial>> getCampaignMaterials(@RequestParam Long userId) {
        List<CampaignMaterial> materials = campaignMaterialService.getCampaignMaterialsByCandidate(userId);
        return ResponseEntity.ok(materials);
    }
    
    @GetMapping("/campaign-materials/type/{type}")
    public ResponseEntity<List<CampaignMaterial>> getCampaignMaterialsByType(
            @RequestParam Long userId, 
            @PathVariable String type) {
        List<CampaignMaterial> materials = campaignMaterialService.getCampaignMaterialsByCandidate(userId);
        return ResponseEntity.ok(materials);
    }
    
    
    // Manifesto announcement
    @PostMapping("/campaign-materials/announcement")
    public ResponseEntity<CampaignMaterial> createAnnouncement(
            @RequestParam Long userId,
            @RequestBody CampaignMaterialRequest request) {
        CampaignMaterial material = campaignMaterialService.createAnnouncement(
            userId, request.getTitle(), request.getDescription());
        return ResponseEntity.ok(material);
    }
    
    // File uploads (manifesto, poster, video, symbol, etc.)
    @PostMapping("/campaign-materials/upload")
    public ResponseEntity<Map<String, Object>> uploadCampaignMaterial(
            @RequestParam Long userId,
            @RequestParam String type,
            @RequestParam String title,
            @RequestParam("file") MultipartFile file) {
        try {
            logger.debug("Uploading file: {} for userId: {}", file.getOriginalFilename(), userId);
            
            CampaignMaterial.MaterialType materialType = CampaignMaterial.MaterialType.valueOf(type.toUpperCase());
            CampaignMaterial material = campaignMaterialService.uploadFile(userId, materialType, title, file);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("id", material.getId());
            response.put("fileName", material.getFileName());
            response.put("message", "File uploaded successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Upload error", e);
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    @PutMapping("/campaign-materials/{id}")
    public ResponseEntity<CampaignMaterial> updateCampaignMaterial(
            @PathVariable Long id,
            @RequestBody CampaignMaterialRequest request) {
        CampaignMaterial material = campaignMaterialService.updateMaterial(
            id, request.getTitle(), request.getDescription());
        return ResponseEntity.ok(material);
    }
    
    @DeleteMapping("/campaign-materials/{id}")
    public ResponseEntity<Void> deleteCampaignMaterial(@PathVariable Long id) {
        campaignMaterialService.deleteMaterial(id);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/campaign-materials/{id}/view")
    public ResponseEntity<CampaignMaterial> incrementViewCount(@PathVariable Long id) {
        CampaignMaterial material = campaignMaterialService.incrementViewCount(id);
        return ResponseEntity.ok(material);
    }
    
    @PostMapping("/campaign-materials/{id}/like")
    public ResponseEntity<CampaignMaterial> incrementLikeCount(@PathVariable Long id) {
        CampaignMaterial material = campaignMaterialService.incrementLikeCount(id);
        return ResponseEntity.ok(material);
    }
    
    // Get candidate ID for current user
    @GetMapping("/candidate-id")
    public ResponseEntity<Long> getCandidateId(@RequestParam Long userId, @RequestParam Long electionId) {
        List<Candidate> candidates = candidateRepository.findByUserIdAndElectionId(userId, electionId);
        if (!candidates.isEmpty()) {
            return ResponseEntity.ok(candidates.get(0).getId());
        }
        return ResponseEntity.notFound().build();
    }
    
    // Get candidate applications for current user
    @Transactional(readOnly = true)
    @GetMapping("/candidate-applications")
    public ResponseEntity<List<Map<String, Object>>> getCandidateApplications(@RequestParam Long userId) {
        try {
            logger.debug("Fetching candidate applications for user: {}", userId);
            List<Candidate> candidates = candidateRepository.findByUserIdWithDetails(userId);
            logger.debug("Found {} candidate applications", candidates.size());
            
            List<Map<String, Object>> applications = new java.util.ArrayList<>();
            
            for (Candidate candidate : candidates) {
                try {
                    Map<String, Object> application = new HashMap<>();
                    application.put("id", candidate.getId());
                    application.put("userId", userId);
                    
                    // Safely access election data
                    if (candidate.getElection() != null) {
                        application.put("electionId", candidate.getElection().getId());
                        application.put("electionTitle", candidate.getElection().getTitle());
                    } else {
                        application.put("electionId", null);
                        application.put("electionTitle", "Unknown Election");
                    }
                    
                    application.put("partyName", candidate.getPartyName());
                    application.put("status", candidate.getStatus() != null ? candidate.getStatus().toString() : "PENDING");
                    application.put("submittedAt", candidate.getCreatedAt() != null ? candidate.getCreatedAt().toString() : "");
                    applications.add(application);
                    logger.debug("Added candidate application");
                } catch (Exception e) {
                    logger.error("Error processing candidate", e);
                    e.printStackTrace();
                }
            }
            
            System.out.println("Returning " + applications.size() + " applications");
            return ResponseEntity.ok(applications);
        } catch (Exception e) {
            System.err.println("Error in getCandidateApplications: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new java.util.ArrayList<>());
        }
    }
}