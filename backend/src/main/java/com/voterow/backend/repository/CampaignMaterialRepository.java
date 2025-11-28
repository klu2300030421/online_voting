package com.voterow.backend.repository;

import com.voterow.backend.model.CampaignMaterial;
import com.voterow.backend.model.Candidate;
import com.voterow.backend.model.Election;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignMaterialRepository extends JpaRepository<CampaignMaterial, Long> {
    List<CampaignMaterial> findByCandidateOrderByUploadDateDesc(Candidate candidate);
    List<CampaignMaterial> findByElectionOrderByUploadDateDesc(Election election);
    List<CampaignMaterial> findByMaterialTypeOrderByUploadDateDesc(String materialType);
    List<CampaignMaterial> findByElectionAndIsApprovedOrderByUploadDateDesc(Election election, Boolean isApproved);
    List<CampaignMaterial> findByCandidateAndElectionOrderByUploadDateDesc(Candidate candidate, Election election);
}