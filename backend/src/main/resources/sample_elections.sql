-- Sample elections for testing
INSERT INTO elections (title, description, start_date, end_date, status, type, is_active, allow_multiple_votes, min_age, created_at, updated_at) VALUES
('Student Council Election 2024', 'Annual student council election for academic year 2024-2025', '2024-12-01 09:00:00', '2024-12-01 17:00:00', 'SCHEDULED', 'STUDENT_UNION', true, false, 18, NOW(), NOW()),
('Local Body Election', 'Municipal corporation election for local governance', '2024-12-15 08:00:00', '2024-12-15 18:00:00', 'SCHEDULED', 'LOCAL_BODY', true, false, 18, NOW(), NOW()),
('Corporate Board Election', 'Annual board of directors election', '2024-11-30 10:00:00', '2024-11-30 16:00:00', 'ACTIVE', 'CORPORATE', true, false, 21, NOW(), NOW());