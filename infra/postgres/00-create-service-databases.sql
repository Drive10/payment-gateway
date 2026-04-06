-- ============================================================
-- Payment Gateway PostgreSQL Initialization Script
-- ============================================================
-- SECURITY: For production, use Vault or external secrets manager
-- Development password: devpassword (change via environment in production)
-- ============================================================

-- Create databases
SELECT 'CREATE DATABASE paymentdb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'paymentdb')\gexec;
SELECT 'CREATE DATABASE authdb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'authdb')\gexec;
SELECT 'CREATE DATABASE orderdb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'orderdb')\gexec;
SELECT 'CREATE DATABASE notificationdb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'notificationdb')\gexec;
SELECT 'CREATE DATABASE simulatordb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'simulatordb')\gexec;
SELECT 'CREATE DATABASE analyticsdb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'analyticsdb')\gexec;

-- Create roles with known passwords for development
-- In production, override via environment or use Vault
DO $$
BEGIN
    -- Master user (for admin access)
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'payment') THEN
        CREATE ROLE payment WITH LOGIN PASSWORD 'devpassword';
    END IF;
    
    -- Service-specific users
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'auth') THEN
        CREATE ROLE auth WITH LOGIN PASSWORD 'devpassword';
    END IF;
    
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'ord') THEN
        CREATE ROLE ord WITH LOGIN PASSWORD 'devpassword';
    END IF;
    
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'paymentuser') THEN
        CREATE ROLE paymentuser WITH LOGIN PASSWORD 'devpassword';
    END IF;
    
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'notification') THEN
        CREATE ROLE notification WITH LOGIN PASSWORD 'devpassword';
    END IF;
    
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'simulator') THEN
        CREATE ROLE simulator WITH LOGIN PASSWORD 'devpassword';
    END IF;
    
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'analytics') THEN
        CREATE ROLE analytics WITH LOGIN PASSWORD 'devpassword';
    END IF;
END
$$;

-- Grant database access
GRANT ALL PRIVILEGES ON DATABASE paymentdb TO payment;
GRANT ALL PRIVILEGES ON DATABASE authdb TO payment, auth;
GRANT ALL PRIVILEGES ON DATABASE orderdb TO payment, ord;
GRANT ALL PRIVILEGES ON DATABASE notificationdb TO payment, notification;
GRANT ALL PRIVILEGES ON DATABASE simulatordb TO payment, simulator;
GRANT ALL PRIVILEGES ON DATABASE analyticsdb TO payment, analytics;

-- Set database owners
ALTER DATABASE paymentdb OWNER TO payment;
ALTER DATABASE authdb OWNER TO auth;
ALTER DATABASE orderdb OWNER TO ord;
ALTER DATABASE notificationdb OWNER TO notification;
ALTER DATABASE simulatordb OWNER TO simulator;
ALTER DATABASE analyticsdb OWNER TO analytics;

-- Grant schema access
GRANT ALL ON SCHEMA public TO auth;
GRANT ALL ON SCHEMA public TO ord;
GRANT ALL ON SCHEMA public TO paymentuser;
GRANT ALL ON SCHEMA public TO notification;
GRANT ALL ON SCHEMA public TO simulator;
GRANT ALL ON SCHEMA public TO analytics;
GRANT ALL ON SCHEMA public TO payment;

-- PostgreSQL 15+ requires explicit schema grants
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO auth;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO ord;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO paymentuser;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO notification;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO simulator;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO analytics;
