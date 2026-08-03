package com.voterow.backend.service;

import com.voterow.backend.model.Notification;
import com.voterow.backend.model.User;
import com.voterow.backend.model.Vote;
import com.voterow.backend.model.ElectionParticipant;
import com.voterow.backend.repository.NotificationRepository;
import com.voterow.backend.repository.VoteRepository;
import com.voterow.backend.repository.ElectionParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final VoteRepository voteRepository;
    private final ElectionParticipantRepository electionParticipantRepository;
    
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }
    
    public List<Notification> getNotificationsByUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    public Optional<Notification> getNotificationById(Long id) {
        return notificationRepository.findById(id);
    }
    
    public Notification createNotification(User user, String title, String message, String type) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setNotificationType(Notification.NotificationType.GENERAL);
        notification.setCreatedAt(LocalDateTime.now());
        
        return notificationRepository.save(notification);
    }
    
    public void sendElectionNotification(User user, String title, String message, String type) {
        createNotification(user, title, message, type);
    }
    
    public void sendBulkNotification(List<User> users, String title, String message, String type) {
        for (User user : users) {
            createNotification(user, title, message, type);
        }
    }
    
    public Notification markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Notification not found"));
        
        notification.setIsRead(true);
        return notificationRepository.save(notification);
    }
    
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }
    
    public void sendVoteConfirmation(User user, com.voterow.backend.model.Election election, Long voteId) {
        String title = "Vote Confirmation";
        String message = String.format("Your vote has been successfully cast in the election: %s. Vote ID: %d", 
            election.getTitle(), voteId);
        createNotification(user, title, message, "VOTE_CONFIRMATION");
    }
    
    public void sendVoterAccessNotification(User user, com.voterow.backend.model.Election election, String accessToken) {
        String title = "Election Access";
        String message = String.format("You are now eligible to vote in the election: %s. Access Token: %s", 
            election.getTitle(), accessToken);
        createNotification(user, title, message, "ELECTION_ACCESS");
    }
    
    public void sendResultsNotification(com.voterow.backend.model.Election election, List<Object[]> results) {
        String title = "Election Results Published";
        String message = String.format("Results for the election '%s' have been published. Check the results page for details.", 
            election.getTitle());
        
        // Get all users who should be notified (voters and candidates)
        Set<User> usersToNotify = new HashSet<>();
        
        // Get all voters who voted in this election
        List<Vote> votes = voteRepository.findByElection(election);
        for (Vote vote : votes) {
            usersToNotify.add(vote.getVoter());
        }
        
        // Get all candidates who participated
        List<ElectionParticipant> participants = electionParticipantRepository.findByElection(election);
        for (ElectionParticipant participant : participants) {
            if (participant.getCandidate() != null && participant.getCandidate().getUser() != null) {
                usersToNotify.add(participant.getCandidate().getUser());
            }
        }
        
        // Send notifications to all users
        for (User user : usersToNotify) {
            createNotification(user, title, message, "RESULTS");
        }
    }
}