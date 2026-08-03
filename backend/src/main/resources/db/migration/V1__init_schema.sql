-- V1__init_schema.sql
-- Initial schema creation for VoteRow voting system

-- Users table
CREATE TABLE IF NOT EXISTS users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  full_name VARCHAR(255) NOT NULL,
  email VARCHAR(255) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  age INT,
  phone_number VARCHAR(15),
  party_name VARCHAR(100),
  id_proof_number VARCHAR(20),
  address VARCHAR(500),
  user_type VARCHAR(50) NOT NULL,
  admin_role VARCHAR(50),
  is_active BOOLEAN DEFAULT true,
  is_verified BOOLEAN DEFAULT false,
  two_factor_enabled BOOLEAN DEFAULT false,
  otp_code VARCHAR(10),
  otp_expiry TIMESTAMP NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_login_at TIMESTAMP NULL
);

-- Elections table
CREATE TABLE IF NOT EXISTS elections (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  description VARCHAR(1000),
  start_date TIMESTAMP,
  end_date TIMESTAMP,
  status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
  type VARCHAR(50) NOT NULL DEFAULT 'OTHER',
  is_active BOOLEAN DEFAULT true,
  allow_multiple_votes BOOLEAN DEFAULT false,
  min_age INT DEFAULT 18,
  eligibility_rules VARCHAR(1000),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  created_by BIGINT,
  FOREIGN KEY (created_by) REFERENCES users(id)
);

-- Candidates table
CREATE TABLE IF NOT EXISTS candidates (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  election_id BIGINT NOT NULL,
  party_name VARCHAR(100) NOT NULL,
  party_symbol VARCHAR(500),
  manifesto VARCHAR(1000),
  qualifications VARCHAR(500),
  status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  approved_by BIGINT,
  approved_at TIMESTAMP NULL,
  rejection_reason VARCHAR(500),
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (election_id) REFERENCES elections(id),
  FOREIGN KEY (approved_by) REFERENCES users(id)
);

-- Votes table
CREATE TABLE IF NOT EXISTS votes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  voter_id BIGINT NOT NULL,
  candidate_id BIGINT NOT NULL,
  election_id BIGINT NOT NULL,
  vote_hash VARCHAR(255),
  encrypted_vote VARCHAR(1000),
  voted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY unique_voter_election (voter_id, election_id),
  FOREIGN KEY (voter_id) REFERENCES users(id),
  FOREIGN KEY (candidate_id) REFERENCES candidates(id),
  FOREIGN KEY (election_id) REFERENCES elections(id)
);

-- Audit logs table
CREATE TABLE IF NOT EXISTS audit_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT,
  action VARCHAR(255) NOT NULL,
  entity_type VARCHAR(100),
  entity_id BIGINT,
  ip_address VARCHAR(45),
  user_agent VARCHAR(500),
  details TEXT,
  log_level VARCHAR(20) DEFAULT 'INFO',
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Campaign materials table
CREATE TABLE IF NOT EXISTS campaign_materials (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  candidate_id BIGINT,
  election_id BIGINT,
  file_name VARCHAR(255),
  file_path VARCHAR(500),
  material_type VARCHAR(50),
  access_level VARCHAR(50) DEFAULT 'PUBLIC',
  uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (candidate_id) REFERENCES candidates(id),
  FOREIGN KEY (election_id) REFERENCES elections(id)
);

-- Election voters table
CREATE TABLE IF NOT EXISTS election_voters (
  election_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  PRIMARY KEY (election_id, user_id),
  FOREIGN KEY (election_id) REFERENCES elections(id),
  FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Election participants table
CREATE TABLE IF NOT EXISTS election_participants (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  election_id BIGINT NOT NULL,
  candidate_id BIGINT,
  user_id BIGINT,
  assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  is_active BOOLEAN DEFAULT true,
  FOREIGN KEY (election_id) REFERENCES elections(id),
  FOREIGN KEY (candidate_id) REFERENCES candidates(id),
  FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Election voter access table (for tracking voter access tokens)
CREATE TABLE IF NOT EXISTS election_voter_access (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  election_id BIGINT NOT NULL,
  voter_id BIGINT NOT NULL,
  access_token VARCHAR(255) UNIQUE,
  granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  expires_at TIMESTAMP NULL,
  is_active BOOLEAN DEFAULT true,
  FOREIGN KEY (election_id) REFERENCES elections(id),
  FOREIGN KEY (voter_id) REFERENCES users(id)
);
