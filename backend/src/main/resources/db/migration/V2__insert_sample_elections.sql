-- V2__insert_sample_elections.sql
-- Sample elections for testing (automatically executed by Flyway on startup)

-- Only insert if table is empty to avoid duplicates

-- Sample Election 1: Draft State
INSERT INTO elections (title, description, start_date, end_date, status, type, is_active, allow_multiple_votes, min_age, eligibility_rules, created_at, updated_at) 
SELECT 'Student Council Election 2024', 
       'Annual student council election for academic year 2024-2025. Candidates from all faculties are invited to apply.',
       DATE_ADD(NOW(), INTERVAL 7 DAY),
       DATE_ADD(NOW(), INTERVAL 8 DAY),
       'DRAFT', 
       'STUDENT_UNION', 
       true, 
       false, 
       18,
       'Must be a registered student with valid ID. Age must be 18 or above.',
       NOW(), 
       NOW()
WHERE NOT EXISTS (SELECT 1 FROM elections WHERE title = 'Student Council Election 2024');

-- Sample Election 2: Published State
INSERT INTO elections (title, description, start_date, end_date, status, type, is_active, allow_multiple_votes, min_age, eligibility_rules, created_at, updated_at) 
SELECT 'Local Body Election',
       'Municipal corporation election for local governance. Vote for your preferred candidates.',
       DATE_ADD(NOW(), INTERVAL 14 DAY),
       DATE_ADD(NOW(), INTERVAL 15 DAY),
       'RESULTS_PUBLISHED',
       'LOCAL_BODY',
       true,
       false,
       18,
       'All registered voters within the municipality are eligible. Age must be 18 or above.',
       NOW(),
       NOW()
WHERE NOT EXISTS (SELECT 1 FROM elections WHERE title = 'Local Body Election');

-- Sample Election 3: Active State  
INSERT INTO elections (title, description, start_date, end_date, status, type, is_active, allow_multiple_votes, min_age, eligibility_rules, created_at, updated_at) 
SELECT 'Corporate Board Election',
       'Annual board of directors election for company governance. All shareholders are eligible to vote.',
       DATE_SUB(NOW(), INTERVAL 2 DAY),
       DATE_ADD(NOW(), INTERVAL 3 DAY),
       'ACTIVE',
       'CORPORATE',
       true,
       false,
       21,
       'Only company shareholders with voting rights are eligible.',
       NOW(),
       NOW()
WHERE NOT EXISTS (SELECT 1 FROM elections WHERE title = 'Corporate Board Election');

-- Sample Election 4: Completed State
INSERT INTO elections (title, description, start_date, end_date, status, type, is_active, allow_multiple_votes, min_age, eligibility_rules, created_at, updated_at) 
SELECT 'General Assembly Election',
       'General assembly member election for organization governance.',
       DATE_SUB(NOW(), INTERVAL 15 DAY),
       DATE_SUB(NOW(), INTERVAL 14 DAY),
       'COMPLETED',
       'GENERAL_ELECTION',
       true,
       false,
       18,
       'All active members of the organization with 1 year membership are eligible.',
       NOW(),
       NOW()
WHERE NOT EXISTS (SELECT 1 FROM elections WHERE title = 'General Assembly Election');
