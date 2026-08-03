package com.voterow.backend.service;

import com.voterow.backend.model.CampaignMaterial;
import com.voterow.backend.model.Candidate;
import com.voterow.backend.repository.CampaignMaterialRepository;
import com.voterow.backend.repository.CandidateRepository;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CampaignMaterialService {

    private final CampaignMaterialRepository campaignMaterialRepository;
    private final CandidateRepository candidateRepository;

    private static final String UPLOAD_DIR = "uploads/campaign-materials/";

    public List<CampaignMaterial> getCampaignMaterialsByCandidate(Long candidateId) {
        return candidateRepository.findById(candidateId)
                .map(campaignMaterialRepository::findByCandidateOrderByUploadDateDesc)
                .orElse(List.of());
    }

    public CampaignMaterial createAnnouncement(Long candidateId, String title, String content) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new RuntimeException("Candidate not found: " + candidateId));
        CampaignMaterial material = new CampaignMaterial();
        material.setCandidate(candidate);
        material.setElection(candidate.getElection());
        material.setTitle(title);
        material.setDescription(content);
        material.setMaterialType("ANNOUNCEMENT");
        material.setUploadDate(LocalDateTime.now());
        material.setIsApproved(false);
        return campaignMaterialRepository.save(material);
    }

    public CampaignMaterial uploadFile(Long candidateId, CampaignMaterial.MaterialType type, String title, MultipartFile file) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new RuntimeException("Candidate not found: " + candidateId));
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            String original = file.getOriginalFilename();
            String ext = (original != null && original.contains("."))
                    ? original.substring(original.lastIndexOf(".")) : "";
            String uniqueName = candidateId + "_" + System.currentTimeMillis() + ext;
            Path dest = uploadPath.resolve(uniqueName);
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

            CampaignMaterial material = new CampaignMaterial();
            material.setCandidate(candidate);
            material.setElection(candidate.getElection());
            material.setMaterialType(type.name());
            material.setTitle(title);
            material.setFileName(uniqueName);
            material.setFilePath(dest.toString());
            material.setFileSize(file.getSize());
            material.setMimeType(file.getContentType());
            material.setUploadDate(LocalDateTime.now());
            material.setIsApproved(false);
            return campaignMaterialRepository.save(material);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }

    public CampaignMaterial updateMaterial(Long id, String title, String content) {
        CampaignMaterial material = campaignMaterialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Material not found: " + id));
        if (title != null) material.setTitle(title);
        if (content != null) material.setDescription(content);
        return campaignMaterialRepository.save(material);
    }

    public void deleteMaterial(Long id) {
        campaignMaterialRepository.findById(id).ifPresent(m -> {
            if (m.getFilePath() != null) {
                try { Files.deleteIfExists(Paths.get(m.getFilePath())); } catch (IOException ignored) {}
            }
            campaignMaterialRepository.delete(m);
        });
    }

    public CampaignMaterial incrementViewCount(Long id) {
        CampaignMaterial material = campaignMaterialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Material not found: " + id));
        material.setViewCount((material.getViewCount() == null ? 0 : material.getViewCount()) + 1);
        return campaignMaterialRepository.save(material);
    }

    public CampaignMaterial incrementLikeCount(Long id) {
        CampaignMaterial material = campaignMaterialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Material not found: " + id));
        material.setLikeCount((material.getLikeCount() == null ? 0 : material.getLikeCount()) + 1);
        return campaignMaterialRepository.save(material);
    }
}