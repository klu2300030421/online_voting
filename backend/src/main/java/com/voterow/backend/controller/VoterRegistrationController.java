package com.voterow.backend.controller;

import com.voterow.backend.model.*;
import com.voterow.backend.repository.*;
import com.voterow.backend.service.ElectionWorkflowService;
import com.voterow.backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/voter-registration")
public class VoterRegistrationController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ElectionRepository electionRepository;
    
    @Autowired
    private VoterAccessRepository voterAccessRepository;
    
    @Autowired
    private ElectionWorkflowService electionWorkflowService;
    
    @Autowired
    private NotificationService notificationService;

    @PostMapping("/elections/{electionId}/register")
    public ResponseEntity<Map<String, Object>> registerForElection(@PathVariable Long electionId, Principal principal) {
        try {
            User voter = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Voter not found"));
            
            Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new RuntimeException("Election not found"));

            // Validate voter eligibility
            if (!electionWorkflowService.validateVoterEligibility(voter, election)) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "You are not eligible for this election");
                return ResponseEntity.badRequest().body(response);
            }

            // Generate access token
            String accessToken = UUID.randomUUID().toString();
            VoterAccess voterAccess = new VoterAccess(voter, election, accessToken);
            voterAccessRepository.save(voterAccess);

            // Send access notification
            notificationService.sendVoterAccessNotification(voter, election, accessToken);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Successfully registered for election");
            response.put("accessToken", accessToken);
            response.put("electionId", electionId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Registration failed: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/verify-access")
    public ResponseEntity<Map<String, Object>> verifyVoterAccess(@RequestBody Map<String, String> request) {
        try {
            String accessToken = request.get("accessToken");
            String otpCode = request.get("otpCode");

            VoterAccess voterAccess = voterAccessRepository.findByAccessToken(accessToken)
                .orElseThrow(() -> new RuntimeException("Invalid access token"));

            // Verify OTP if provided
            if (otpCode != null && !otpCode.equals(voterAccess.getOtpCode())) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "Invalid OTP code");
                return ResponseEntity.badRequest().body(response);
            }

            // Check if token is expired or used
            if (voterAccess.getIsUsed() || voterAccess.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "Access token expired or already used");
                return ResponseEntity.badRequest().body(response);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Access verified");
            response.put("voterId", voterAccess.getVoter().getId());
            response.put("electionId", voterAccess.getElection().getId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Verification failed: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/elections/{electionId}/eligibility")
    public ResponseEntity<Map<String, Object>> checkEligibility(@PathVariable Long electionId, Principal principal) {
        try {
            User voter = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Voter not found"));
            
            Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new RuntimeException("Election not found"));

            boolean eligible = electionWorkflowService.validateVoterEligibility(voter, election);

            Map<String, Object> response = new HashMap<>();
            response.put("eligible", eligible);
            response.put("voterId", voter.getId());
            response.put("electionId", electionId);
            response.put("voterName", voter.getFullName());
            response.put("electionTitle", election.getTitle());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Eligibility check failed: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}