package com.voterow.backend.config;

import com.voterow.backend.model.Election;
import com.voterow.backend.model.AdminRole;
import com.voterow.backend.model.User;
import com.voterow.backend.model.UserType;
import com.voterow.backend.repository.ElectionRepository;
import com.voterow.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

import java.time.LocalDateTime;

@Component
@Profile("dev")
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private ElectionRepository electionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        ensureDefaultAdminExists();

        // Check if elections already exist
        long electionCount = electionRepository.count();
        
        if (electionCount == 0) {
            System.out.println("No elections found in database. Creating sample elections...");
            createSampleElections();
        } else {
            System.out.println("Found " + electionCount + " elections in database.");
        }
    }

    private void ensureDefaultAdminExists() {
        User admin = userRepository.findByEmail("admin@test.com").orElseGet(User::new);
        admin.setFullName("Default Admin");
        admin.setEmail("admin@test.com");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setAge(30);
        admin.setUserType(UserType.ROLE_ADMIN);
        admin.setAdminRole(AdminRole.SUPER_ADMIN);
        admin.setIsActive(true);
        admin.setIsVerified(true);
        admin.setTwoFactorEnabled(false);

        if (admin.getCreatedAt() == null) {
            admin.setCreatedAt(LocalDateTime.now());
        }
        admin.setUpdatedAt(LocalDateTime.now());

        userRepository.save(admin);
        System.out.println("✓ Ensured default admin user admin@test.com / admin123");
    }

    private void createSampleElections() {
        // Create sample elections
        Election election1 = new Election();
        election1.setTitle("Student Council Election 2024");
        election1.setDescription("Annual student council election for academic year 2024-2025");
        election1.setStartDate(LocalDateTime.now().plusDays(1));
        election1.setEndDate(LocalDateTime.now().plusDays(7));
        election1.setStatus(Election.ElectionStatus.SCHEDULED);
        election1.setType(Election.ElectionType.STUDENT_UNION);
        election1.setIsActive(true);
        election1.setAllowMultipleVotes(false);
        election1.setMinAge(18);
        election1.setCreatedAt(LocalDateTime.now());
        election1.setUpdatedAt(LocalDateTime.now());

        Election election2 = new Election();
        election2.setTitle("Local Body Election");
        election2.setDescription("Municipal corporation election for local governance");
        election2.setStartDate(LocalDateTime.now().minusHours(1));
        election2.setEndDate(LocalDateTime.now().plusDays(1));
        election2.setStatus(Election.ElectionStatus.ACTIVE);
        election2.setType(Election.ElectionType.LOCAL_BODY);
        election2.setIsActive(true);
        election2.setAllowMultipleVotes(false);
        election2.setMinAge(18);
        election2.setCreatedAt(LocalDateTime.now());
        election2.setUpdatedAt(LocalDateTime.now());

        Election election3 = new Election();
        election3.setTitle("Corporate Board Election");
        election3.setDescription("Annual board of directors election");
        election3.setStartDate(LocalDateTime.now().plusDays(15));
        election3.setEndDate(LocalDateTime.now().plusDays(16));
        election3.setStatus(Election.ElectionStatus.SCHEDULED);
        election3.setType(Election.ElectionType.CORPORATE);
        election3.setIsActive(true);
        election3.setAllowMultipleVotes(false);
        election3.setMinAge(21);
        election3.setCreatedAt(LocalDateTime.now());
        election3.setUpdatedAt(LocalDateTime.now());

        // Save elections
        electionRepository.save(election1);
        electionRepository.save(election2);
        electionRepository.save(election3);

        System.out.println("✓ Created 3 sample elections successfully!");
    }
}
