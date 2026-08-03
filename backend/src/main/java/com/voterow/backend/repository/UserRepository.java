package com.voterow.backend.repository;

import com.voterow.backend.model.AdminRole;
import com.voterow.backend.model.User;
import com.voterow.backend.model.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    List<User> findByUserType(UserType userType);
    List<User> findByUserTypeAndIsActiveAndIsVerified(UserType userType, Boolean isActive, Boolean isVerified);
    
    List<User> findByIsActive(Boolean isActive);
    
    List<User> findByUserTypeAndIsActive(UserType userType, Boolean isActive);
    
    List<User> findByAdminRole(AdminRole adminRole);
    
    List<User> findByIsVerified(Boolean isVerified);
    
    List<User> findByTwoFactorEnabled(Boolean twoFactorEnabled);
    
    List<User> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String fullName, String email);
    
    // Add missing method for counting users by user type
    Long countByUserType(UserType userType);
    
    @Query("SELECT u FROM User u WHERE u.lastLoginAt BETWEEN :startDate AND :endDate")
    List<User> findByLastLoginBetween(@Param("startDate") LocalDateTime startDate, 
                                    @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    List<User> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                    @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.userType = :userType AND u.isActive = true")
    Long countActiveByUserType(@Param("userType") UserType userType);
    
    @Query("SELECT u FROM User u WHERE u.age >= :minAge AND u.isVerified = true AND u.isActive = true")
    List<User> findEligibleVoters(@Param("minAge") Integer minAge);
}