package com.voterow.backend.service;

import com.voterow.backend.model.CampaignMaterial;
import com.voterow.backend.repository.CampaignMaterialRepository;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CampaignMaterialService {
    
    private final CampaignMaterialRepository campaignMaterialRepository;
    
    public List<CampaignMaterial> getCampaignMaterialsByCandidate(Long candidateId) {
        return List.of();
    }

    // Minimal stubs to satisfy controller endpoints; implement persistence later as needed
    public CampaignMaterial createAnnouncement(Long userId, String title, String content) {
        CampaignMaterial material = new CampaignMaterial();
        material.setTitle(title);
        material.setDescription(content);
        return material;
    }

    public CampaignMaterial uploadFile(Long userId, CampaignMaterial.MaterialType type, String title, MultipartFile file) {
        CampaignMaterial material = new CampaignMaterial();
        material.setTitle(title);
        material.setMaterialType(type.name());
        material.setFileName(file != null ? file.getOriginalFilename() : null);
        return material;
    }

    public CampaignMaterial updateMaterial(Long id, String title, String content) {
        CampaignMaterial material = new CampaignMaterial();
        material.setId(id);
        material.setTitle(title);
        material.setDescription(content);
        return material;
    }

    public void deleteMaterial(Long id) {
        // no-op for stub
    }

    public CampaignMaterial incrementViewCount(Long id) {
        CampaignMaterial material = new CampaignMaterial();
        material.setId(id);
        return material;
    }

    public CampaignMaterial incrementLikeCount(Long id) {
        CampaignMaterial material = new CampaignMaterial();
        material.setId(id);
        return material;
    }
}