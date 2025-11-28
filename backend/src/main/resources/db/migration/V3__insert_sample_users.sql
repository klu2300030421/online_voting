-- V3__insert_sample_users.sql
-- Sample users for testing (admin, voters, participants/candidates)
-- All passwords are hashed with BCrypt. Password: "password"
-- Hash: $2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.

-- Admin user
INSERT INTO users (full_name, email, password, age, user_type, admin_role, is_active, is_verified, created_at, updated_at)
SELECT 'Admin User', 'admin@voterow.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 35, 'ROLE_ADMIN', 'SUPER_ADMIN', true, true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@voterow.com');

-- Sample voters
INSERT INTO users (full_name, email, password, age, phone_number, user_type, is_active, is_verified, created_at, updated_at)
SELECT 'John Doe', 'john.doe@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 28, '9876543210', 'ROLE_VOTER', true, true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'john.doe@example.com');

INSERT INTO users (full_name, email, password, age, phone_number, user_type, is_active, is_verified, created_at, updated_at)
SELECT 'Jane Smith', 'jane.smith@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 32, '9876543211', 'ROLE_VOTER', true, true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'jane.smith@example.com');

INSERT INTO users (full_name, email, password, age, phone_number, user_type, is_active, is_verified, created_at, updated_at)
SELECT 'Robert Johnson', 'robert.johnson@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 45, '9876543212', 'ROLE_VOTER', true, true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'robert.johnson@example.com');

INSERT INTO users (full_name, email, password, age, phone_number, user_type, is_active, is_verified, created_at, updated_at)
SELECT 'Alice Brown', 'alice.brown@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 29, '9876543213', 'ROLE_VOTER', true, true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'alice.brown@example.com');

INSERT INTO users (full_name, email, password, age, phone_number, user_type, is_active, is_verified, created_at, updated_at)
SELECT 'Michael Wilson', 'michael.wilson@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 51, '9876543214', 'ROLE_VOTER', true, true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'michael.wilson@example.com');

-- Sample participants/candidates
INSERT INTO users (full_name, email, password, age, phone_number, party_name, user_type, is_active, is_verified, created_at, updated_at)
SELECT 'David Davis', 'david.davis@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 38, '9876543215', 'Progressive Party', 'ROLE_PARTICIPANT', true, true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'david.davis@example.com');

INSERT INTO users (full_name, email, password, age, phone_number, party_name, user_type, is_active, is_verified, created_at, updated_at)
SELECT 'Sarah Miller', 'sarah.miller@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 34, '9876543216', 'Democratic Alliance', 'ROLE_PARTICIPANT', true, true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'sarah.miller@example.com');

INSERT INTO users (full_name, email, password, age, phone_number, party_name, user_type, is_active, is_verified, created_at, updated_at)
SELECT 'James Taylor', 'james.taylor@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 42, '9876543217', 'Independent', 'ROLE_PARTICIPANT', true, true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'james.taylor@example.com');

INSERT INTO users (full_name, email, password, age, phone_number, party_name, user_type, is_active, is_verified, created_at, updated_at)
SELECT 'Emily Anderson', 'emily.anderson@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 31, '9876543218', 'Liberal Front', 'ROLE_PARTICIPANT', true, true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'emily.anderson@example.com');
