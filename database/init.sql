-- PayFlow Dashboard Database Initialization Script
-- This script creates dummy data for testing purposes

-- Create database if it doesn't exist
CREATE DATABASE IF NOT EXISTS payflow_dashboard;
USE payflow_dashboard;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    role ENUM('admin', 'merchant', 'viewer') DEFAULT 'merchant',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Transactions table
CREATE TABLE IF NOT EXISTS transactions (
    id VARCHAR(36) PRIMARY KEY,
    order_reference VARCHAR(100) NOT NULL,
    user_id INT,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'INR',
    status ENUM('CREATED', 'PENDING', 'CAPTURED', 'FAILED', 'REFUNDED', 'PARTIALLY_REFUNDED') DEFAULT 'CREATED',
    provider VARCHAR(50),
    method VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Analytics table (daily aggregates)
CREATE TABLE IF NOT EXISTS daily_analytics (
    id INT AUTO_INCREMENT PRIMARY KEY,
    date DATE NOT NULL UNIQUE,
    total_volume DECIMAL(15,2) DEFAULT 0,
    transaction_count INT DEFAULT 0,
    success_count INT DEFAULT 0,
    failed_count INT DEFAULT 0,
    refunded_count INT DEFAULT 0,
    average_transaction DECIMAL(10,2) DEFAULT 0,
    success_rate DECIMAL(5,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Insert sample users
INSERT IGNORE INTO users (email, name, role) VALUES
('admin@payflow.com', 'Admin User', 'admin'),
('merchant@payflow.com', 'Merchant User', 'merchant'),
('viewer@payflow.com', 'Viewer User', 'viewer'),
('john.doe@example.com', 'John Doe', 'merchant');

-- Get user IDs for foreign key references
SET @admin_id = (SELECT id FROM users WHERE email = 'admin@payflow.com');
SET @merchant_id = (SELECT id FROM users WHERE email = 'merchant@payflow.com');
SET @viewer_id = (SELECT id FROM users WHERE email = 'viewer@payflow.com');
SET @john_id = (SELECT id FROM users WHERE email = 'john.doe@example.com');

-- Insert sample transactions
INSERT IGNORE INTO transactions (id, order_reference, user_id, amount, currency, status, provider, method, created_at) VALUES
('d1111111-1111-1111-1111-111111111111', 'ORD_20260331_001', @john_id, 5000.00, 'INR', 'CAPTURED', 'SIMULATOR', 'CARD', NOW()),
('d2222222-2222-2222-2222-222222222222', 'ORD_20260330_001', @john_id, 12500.00, 'INR', 'CAPTURED', 'RAZORPAY', 'UPI', DATE_SUB(NOW(), INTERVAL 1 DAY)),
('d3333333-3333-3333-3333-333333333333', 'ORD_20260329_001', @john_id, 3500.00, 'INR', 'PARTIALLY_REFUNDED', 'PHONEPE', 'WALLET', DATE_SUB(NOW(), INTERVAL 2 DAY)),
('d4444444-4444-4444-4444-444444444444', 'ORD_20260328_001', @john_id, 8500.00, 'INR', 'CAPTURED', 'SIMULATOR', 'CARD', DATE_SUB(NOW(), INTERVAL 3 DAY)),
('d5555555-5555-5555-5555-555555555555', 'ORD_20260327_001', @john_id, 15000.00, 'INR', 'FAILED', 'RAZORPAY', 'CARD', DATE_SUB(NOW(), INTERVAL 4 DAY)),
('d6666666-6666-6666-6666-666666666666', 'ORD_20260326_001', @john_id, 2500.00, 'INR', 'CREATED', 'SIMULATOR', 'UPI', DATE_SUB(NOW(), INTERVAL 5 DAY)),
('d7777777-7777-7777-7777-777777777777', 'ORD_20260325_001', @john_id, 7500.00, 'INR', 'REFUNDED', 'PHONEPE', 'CARD', DATE_SUB(NOW(), INTERVAL 6 DAY)),
('d8888888-8888-8888-8888-888888888888', 'ORD_20260324_001', @john_id, 10000.00, 'INR', 'CAPTURED', 'SIMULATOR', 'UPI', DATE_SUB(NOW(), INTERVAL 7 DAY));

-- Insert sample daily analytics for the last 7 days
INSERT IGNORE INTO daily_analytics (date, total_volume, transaction_count, success_count, failed_count, refunded_count, average_transaction, success_rate)
SELECT 
    DATE(DATE_SUB(NOW(), INTERVAL (7 - seq) DAY)) as date,
    FLOOR(RAND() * 500000) + 100000 as total_volume,
    FLOOR(RAND() * 50) + 10 as transaction_count,
    FLOOR(RAND() * 40) + 5 as success_count,
    FLOOR(RAND() * 10) as failed_count,
    FLOOR(RAND() * 5) as refunded_count,
    ROUND((FLOOR(RAND() * 500000) + 100000) / GREATEST(FLOOR(RAND() * 50) + 10, 1), 2) as average_transaction,
    ROUND((FLOOR(RAND() * 40) + 5) / GREATEST(FLOOR(RAND() * 50) + 10, 1) * 100, 2) as success_rate
FROM 
    (SELECT @row := @row + 1 as seq FROM 
        (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7) t1,
        (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3) t2,
        (SELECT @row:=0) numbers
    ) seq_table
WHERE seq BETWEEN 1 AND 7
ON DUPLICATE KEY UPDATE
    total_volume = VALUES(total_volume),
    transaction_count = VALUES(transaction_count),
    success_count = VALUES(success_count),
    failed_count = VALUES(failed_count),
    refunded_count = VALUES(refunded_count),
    average_transaction = VALUES(average_transaction),
    success_rate = VALUES(success_rate);

-- Create a simple view for dashboard stats
CREATE OR REPLACE VIEW dashboard_stats AS
SELECT 
    SUM(CASE WHEN t.status = 'CAPTURED' THEN t.amount ELSE 0 END) as total_revenue,
    COUNT(t.id) as total_transactions,
    SUM(CASE WHEN t.status = 'CAPTURED' THEN 1 ELSE 0 END) as successful_transactions,
    SUM(CASE WHEN t.status IN ('FAILED', 'PARTIALLY_REFUNDED') THEN 1 ELSE 0 END) as failed_transactions,
    ROUND(
        (SUM(CASE WHEN t.status = 'CAPTURED' THEN 1 ELSE 0 END) / 
        GREATEST(COUNT(t.id), 1)) * 100, 2
    ) as success_rate
FROM transactions t
WHERE t.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY);

-- Create a view for recent transactions
CREATE OR REPLACE VIEW recent_transactions AS
SELECT 
    t.id,
    t.order_reference,
    u.name as user_name,
    t.amount,
    t.currency,
    t.status,
    t.provider,
    t.method,
    t.created_at
FROM transactions t
JOIN users u ON t.user_id = u.id
ORDER BY t.created_at DESC
LIMIT 50;

-- Create indexes for better performance
CREATE INDEX idx_transactions_user_id ON transactions(user_id);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
CREATE INDEX idx_daily_analytics_date ON daily_analytics(date);

-- Show completion message
SELECT 'Database initialization completed successfully!' as message;
SELECT 'Sample data includes:' as info;
SELECT CONCAT('- ', COUNT(*), ' users') FROM users;
SELECT CONCAT('- ', COUNT(*), ' transactions') FROM transactions;
SELECT CONCAT('- ', COUNT(*), ' days of analytics') FROM daily_analytics;