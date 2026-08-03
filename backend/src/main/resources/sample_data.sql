INSERT IGNORE INTO users (id, full_name, email, password, age, user_type, admin_role, is_active, is_verified, two_factor_enabled, created_at, updated_at) VALUES
(1, 'Admin User', 'admin@test.com', '$2a$10$LczMtM4iDTOiUFyeS7DVYOWjPdP9vbUexbXNv.YBQjdrBQLPDhtLa', 30, 'ROLE_ADMIN', 'SUPER_ADMIN', true, true, false, NOW(), NOW()),
(2, 'John Voter', 'voter@test.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi.', 25, 'ROLE_VOTER', NULL, true, true, false, NOW(), NOW()),
(3, 'Jane Candidate', 'candidate@test.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi.', 28, 'ROLE_PARTICIPANT', NULL, true, true, false, NOW(), NOW());

INSERT IGNORE INTO elections (id, title, description, start_date, end_date, status, type, is_active, allow_multiple_votes, min_age, created_at, updated_at) VALUES
(1, 'Student Council Election 2024', 'Annual student council election', '2024-11-01 09:00:00', '2024-12-31 17:00:00', 'ACTIVE', 'STUDENT_UNION', true, false, 18, NOW(), NOW()),
(2, 'Presidential Election 2024', 'National presidential election', '2024-11-01 08:00:00', '2024-12-31 18:00:00', 'ACTIVE', 'GENERAL_ELECTION', true, false, 18, NOW(), NOW());

INSERT IGNORE INTO candidates (id, user_id, election_id, party_name, status, is_active, created_at, updated_at) VALUES
(1, 3, 1, 'Progressive Party', 'APPROVED', true, NOW(), NOW()),
(2, 3, 2, 'Independent', 'APPROVED', true, NOW(), NOW());

INSERT IGNORE INTO election_participants (id, election_id, candidate_id, assigned_at, is_active) VALUES
(1, 1, 1, NOW(), true),
(2, 2, 2, NOW(), true);