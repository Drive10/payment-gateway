-- ===========================================
-- PayFlow Database Initialization
-- ===========================================

-- Create databases for each service
CREATE DATABASE auth_db;
CREATE DATABASE order_db;
CREATE DATABASE payment_db;
CREATE DATABASE notification_db;
CREATE DATABASE simulator_db;
CREATE DATABASE analytics_db;

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE auth_db TO payflow;
GRANT ALL PRIVILEGES ON DATABASE order_db TO payflow;
GRANT ALL PRIVILEGES ON DATABASE payment_db TO payflow;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO payflow;
GRANT ALL PRIVILEGES ON DATABASE simulator_db TO payflow;
GRANT ALL PRIVILEGES ON DATABASE analytics_db TO payflow;
