-- Fix database schema issues
DROP TABLE IF EXISTS election_participants;
CREATE TABLE election_participants (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    election_id BIGINT,
    candidate_id BIGINT,
    user_id BIGINT,
    assigned_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (election_id) REFERENCES elections(id) ON DELETE CASCADE,
    FOREIGN KEY (candidate_id) REFERENCES candidates(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

DROP TABLE IF EXISTS election_voters;
CREATE TABLE election_voters (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    election_id BIGINT,
    voter_id BIGINT,
    registered_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_eligible BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (election_id) REFERENCES elections(id) ON DELETE CASCADE,
    FOREIGN KEY (voter_id) REFERENCES users(id) ON DELETE CASCADE
);