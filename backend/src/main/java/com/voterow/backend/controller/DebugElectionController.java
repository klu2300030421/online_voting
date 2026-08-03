package com.voterow.backend.controller;

import com.voterow.backend.model.Election;
import com.voterow.backend.repository.ElectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugElectionController {

    @Autowired
    private ElectionRepository electionRepository;

    @GetMapping("/elections/status")
    public ResponseEntity<Map<String, Object>> getElectionStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            long totalElections = electionRepository.count();
            List<Election> activeElections = electionRepository.findByIsActiveTrueOrderByCreatedAtDesc();
            List<Election> allElections = electionRepository.findAll();
            
            status.put("totalElections", totalElections);
            status.put("activeElections", activeElections.size());
            status.put("allElectionsCount", allElections.size());
            
            // Add details about each election
            for (int i = 0; i < allElections.size() && i < 5; i++) {
                Election e = allElections.get(i);
                Map<String, Object> electionInfo = new HashMap<>();
                electionInfo.put("id", e.getId());
                electionInfo.put("title", e.getTitle());
                electionInfo.put("isActive", e.getIsActive());
                electionInfo.put("status", e.getStatus());
                status.put("election" + (i + 1), electionInfo);
            }
            
            status.put("timestamp", LocalDateTime.now());
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            status.put("error", e.getMessage());
            return ResponseEntity.status(500).body(status);
        }
    }

    @PostMapping("/elections/create-sample")
    public ResponseEntity<Map<String, Object>> createSampleElection() {
        try {
            Election election = new Election();
            election.setTitle("Debug Test Election " + System.currentTimeMillis());
            election.setDescription("Test election created via debug endpoint");
            election.setStartDate(LocalDateTime.now().plusHours(1));
            election.setEndDate(LocalDateTime.now().plusDays(1));
            election.setStatus(Election.ElectionStatus.SCHEDULED);
            election.setType(Election.ElectionType.OTHER);
            election.setIsActive(true);
            election.setAllowMultipleVotes(false);
            election.setMinAge(18);
            election.setCreatedAt(LocalDateTime.now());
            election.setUpdatedAt(LocalDateTime.now());

            Election saved = electionRepository.save(election);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Sample election created");
            response.put("electionId", saved.getId());
            response.put("title", saved.getTitle());
            response.put("isActive", saved.getIsActive());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}