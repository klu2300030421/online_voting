package com.voterow.backend.config;

import com.voterow.backend.model.AdminRole;
import com.voterow.backend.model.Election;
import com.voterow.backend.model.User;
import com.voterow.backend.model.UserType;
import com.voterow.backend.repository.ElectionRepository;
import com.voterow.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class AdminSeedInitializer {

    @Bean
    CommandLineRunner seedDefaultAdmin(UserRepository userRepository, ElectionRepository electionRepository, PasswordEncoder passwordEncoder) {
        return args -> {
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

            if (electionRepository.count() == 0) {
                Election election1 = new Election();
                election1.setTitle("Mandal Election");
                election1.setDescription("Default election for candidate application testing");
                election1.setStartDate(LocalDateTime.now().minusDays(1));
                election1.setEndDate(LocalDateTime.now().plusDays(7));
                election1.setStatus(Election.ElectionStatus.ACTIVE);
                election1.setType(Election.ElectionType.LOCAL_BODY);
                election1.setIsActive(true);
                election1.setAllowMultipleVotes(false);
                election1.setMinAge(18);
                election1.setCreatedAt(LocalDateTime.now());
                election1.setUpdatedAt(LocalDateTime.now());

                Election election2 = new Election();
                election2.setTitle("Student Council Election");
                election2.setDescription("Default upcoming election for testing");
                election2.setStartDate(LocalDateTime.now().plusDays(2));
                election2.setEndDate(LocalDateTime.now().plusDays(10));
                election2.setStatus(Election.ElectionStatus.SCHEDULED);
                election2.setType(Election.ElectionType.STUDENT_UNION);
                election2.setIsActive(true);
                election2.setAllowMultipleVotes(false);
                election2.setMinAge(18);
                election2.setCreatedAt(LocalDateTime.now());
                election2.setUpdatedAt(LocalDateTime.now());

                electionRepository.save(election1);
                electionRepository.save(election2);
                System.out.println("✓ Seeded default elections for application testing");
            }
        };
    }
}