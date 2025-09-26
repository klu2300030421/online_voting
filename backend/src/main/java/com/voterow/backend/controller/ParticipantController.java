package com.voterow.backend.controller;

import com.voterow.backend.model.Election;
import com.voterow.backend.model.User;
import com.voterow.backend.repository.UserRepository;
import com.voterow.backend.service.ElectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/participant")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PARTICIPANT')")
public class ParticipantController {

    // These must be final
    private final ElectionService electionService;
    private final UserRepository userRepository;

    @PostMapping("/elections/{id}/enroll")
    public ResponseEntity<Election> enrollInElection(@PathVariable Long id, Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName()).orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(electionService.enrollParticipant(id, currentUser));
    }
}