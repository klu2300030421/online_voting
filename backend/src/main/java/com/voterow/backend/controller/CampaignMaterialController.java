package com.voterow.backend.controller;

import com.voterow.backend.model.CampaignMaterial;
import com.voterow.backend.model.Candidate;
import com.voterow.backend.model.Election;
import com.voterow.backend.repository.CampaignMaterialRepository;
import com.voterow.backend.repository.CandidateRepository;
import com.voterow.backend.repository.ElectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/campaign-materials")
public class CampaignMaterialController {
    
    @Autowired
    private CampaignMaterialRepository campaignMaterialRepository;
    
    @Autowired
    private CandidateRepository candidateRepository;
    
    @Autowired
    private ElectionRepository electionRepository;
    
    private final String uploadDir = "uploads/campaign-materials/";
    
    /**
     * Get all campaign materials for a candidate
     */
    @GetMapping("/candidate/{candidateId}")
    public ResponseEntity<List<CampaignMaterial>> getCandidateMaterials(@PathVariable Long candidateId) {
        Optional<Candidate> candidateOpt = candidateRepository.findById(candidateId);
        if (!candidateOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        List<CampaignMaterial> materials = campaignMaterialRepository.findByCandidateOrderByUploadDateDesc(candidateOpt.get());
        return ResponseEntity.ok(materials);
    }
    
    /**
     * Get all campaign materials for an election
     */
    @GetMapping("/election/{electionId}")
    public ResponseEntity<List<CampaignMaterial>> getElectionMaterials(@PathVariable Long electionId) {
        Optional<Election> electionOpt = electionRepository.findById(electionId);
        if (!electionOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        List<CampaignMaterial> materials = campaignMaterialRepository.findByElectionOrderByUploadDateDesc(electionOpt.get());
        return ResponseEntity.ok(materials);
    }
    
    /**
     * Get campaign materials by type
     */
    @GetMapping("/type/{materialType}")
    public ResponseEntity<List<CampaignMaterial>> getMaterialsByType(@PathVariable String materialType) {
        List<CampaignMaterial> materials = campaignMaterialRepository.findByMaterialTypeOrderByUploadDateDesc(materialType);
        return ResponseEntity.ok(materials);
    }
    
    /**
     * Get approved campaign materials for an election
     */
    @GetMapping("/election/{electionId}/approved")
    public ResponseEntity<List<CampaignMaterial>> getApprovedMaterials(@PathVariable Long electionId) {
        Optional<Election> electionOpt = electionRepository.findById(electionId);
        if (!electionOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        List<CampaignMaterial> materials = campaignMaterialRepository.findByElectionAndIsApprovedOrderByUploadDateDesc(electionOpt.get(), true);
        return ResponseEntity.ok(materials);
    }
    
    /**
     * Upload campaign material
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadCampaignMaterial(
            @RequestParam("file") MultipartFile file,
            @RequestParam("candidateId") Long candidateId,
            @RequestParam("electionId") Long electionId,
            @RequestParam("materialType") String materialType,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate candidate and election
            Optional<Candidate> candidateOpt = candidateRepository.findById(candidateId);
            Optional<Election> electionOpt = electionRepository.findById(electionId);
            
            if (!candidateOpt.isPresent()) {
                response.put("error", "Candidate not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!electionOpt.isPresent()) {
                response.put("error", "Election not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validate file
            if (file.isEmpty()) {
                response.put("error", "File is empty");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            String uniqueFilename = candidateId + "_" + electionId + "_" + System.currentTimeMillis() + fileExtension;
            Path filePath = uploadPath.resolve(uniqueFilename);
            
            // Save file
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Create campaign material record
            CampaignMaterial material = new CampaignMaterial();
            material.setCandidate(candidateOpt.get());
            material.setElection(electionOpt.get());
            material.setMaterialType(materialType);
            material.setTitle(title != null ? title : originalFilename);
            material.setDescription(description);
            material.setFilePath(filePath.toString());
            material.setFileName(uniqueFilename);
            material.setFileSize(file.getSize());
            material.setMimeType(file.getContentType());
            material.setUploadDate(LocalDateTime.now());
            material.setIsApproved(false); // Requires approval by default
            
            CampaignMaterial savedMaterial = campaignMaterialRepository.save(material);
            
            response.put("success", true);
            response.put("message", "Campaign material uploaded successfully");
            response.put("materialId", savedMaterial.getId());
            response.put("fileName", uniqueFilename);
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            response.put("error", "Failed to upload file");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Download campaign material file
     */
    @GetMapping("/download/{materialId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long materialId) {
        try {
            Optional<CampaignMaterial> materialOpt = campaignMaterialRepository.findById(materialId);
            if (!materialOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            CampaignMaterial material = materialOpt.get();
            Path filePath = Paths.get(material.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(material.getMimeType()))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + material.getFileName() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Approve campaign material
     */
    @PutMapping("/{materialId}/approve")
    public ResponseEntity<Map<String, Object>> approveMaterial(@PathVariable Long materialId) {
        Map<String, Object> response = new HashMap<>();
        
        Optional<CampaignMaterial> materialOpt = campaignMaterialRepository.findById(materialId);
        if (!materialOpt.isPresent()) {
            response.put("error", "Campaign material not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        
        CampaignMaterial material = materialOpt.get();
        material.setIsApproved(true);
        material.setApprovalDate(LocalDateTime.now());
        
        campaignMaterialRepository.save(material);
        
        response.put("success", true);
        response.put("message", "Campaign material approved successfully");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Reject campaign material
     */
    @PutMapping("/{materialId}/reject")
    public ResponseEntity<Map<String, Object>> rejectMaterial(
            @PathVariable Long materialId,
            @RequestBody Map<String, String> rejectionData) {
        
        Map<String, Object> response = new HashMap<>();
        
        Optional<CampaignMaterial> materialOpt = campaignMaterialRepository.findById(materialId);
        if (!materialOpt.isPresent()) {
            response.put("error", "Campaign material not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        
        CampaignMaterial material = materialOpt.get();
        material.setIsApproved(false);
        material.setRejectionReason(rejectionData.get("reason"));
        
        campaignMaterialRepository.save(material);
        
        response.put("success", true);
        response.put("message", "Campaign material rejected");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Delete campaign material
     */
    @DeleteMapping("/{materialId}")
    public ResponseEntity<Map<String, Object>> deleteMaterial(@PathVariable Long materialId) {
        Map<String, Object> response = new HashMap<>();
        
        Optional<CampaignMaterial> materialOpt = campaignMaterialRepository.findById(materialId);
        if (!materialOpt.isPresent()) {
            response.put("error", "Campaign material not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        
        CampaignMaterial material = materialOpt.get();
        
        try {
            // Delete physical file
            Path filePath = Paths.get(material.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
            
            // Delete database record
            campaignMaterialRepository.delete(material);
            
            response.put("success", true);
            response.put("message", "Campaign material deleted successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            response.put("error", "Failed to delete file");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Update campaign material metadata
     */
    @PutMapping("/{materialId}")
    public ResponseEntity<Map<String, Object>> updateMaterial(
            @PathVariable Long materialId,
            @RequestBody Map<String, String> updateData) {
        
        Map<String, Object> response = new HashMap<>();
        
        Optional<CampaignMaterial> materialOpt = campaignMaterialRepository.findById(materialId);
        if (!materialOpt.isPresent()) {
            response.put("error", "Campaign material not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        
        CampaignMaterial material = materialOpt.get();
        
        if (updateData.containsKey("title")) {
            material.setTitle(updateData.get("title"));
        }
        if (updateData.containsKey("description")) {
            material.setDescription(updateData.get("description"));
        }
        if (updateData.containsKey("materialType")) {
            material.setMaterialType(updateData.get("materialType"));
        }
        
        campaignMaterialRepository.save(material);
        
        response.put("success", true);
        response.put("message", "Campaign material updated successfully");
        
        return ResponseEntity.ok(response);
    }
}