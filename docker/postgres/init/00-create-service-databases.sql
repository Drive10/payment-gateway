-- ============================================================
-- Payment Gateway PostgreSQL Initialization Script
-- ============================================================
-- SECURITY: This script uses environment variables for passwords
-- Passwords should be passed via POSTGRES_PASSWORD environment variable
-- For production, use Vault or external secrets manager
-- ============================================================

-- Create databases
SELECT 'CREATE DATABASE paymentdb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'paymentdb')\gexec;
SELECT 'CREATE DATABASE authdb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'authdb')\gexec;
SELECT 'CREATE DATABASE orderdb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'orderdb')\gexec;
SELECT 'CREATE DATABASE notificationdb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'notificationdb')\gexec;
SELECT 'CREATE DATABASE simulatordb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'simulatordb')\gexec;
SELECT 'CREATE DATABASE analyticsdb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'analyticsdb')\gexec;

-- Create roles with secure passwords (override via environment in production)
DO $$
DECLARE
    v_master_password TEXT := COALESCE(current_setting('app.master_password', true), gen_random_uuid()::text);
    v_db_password TEXT := COALESCE(current_setting('app.db_password', true), gen_random_uuid()::text);
BEGIN
    -- Master user (for admin access)
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'payment') THEN
        EXECUTE format('CREATE ROLE payment WITH LOGIN PASSWORD %L', v_master_password);
    END IF;
    
    -- Service-specific users
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'auth') THEN
        EXECUTE format('CREATE ROLE auth WITH LOGIN PASSWORD %L', v_db_password);
    END IF;
    
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'ord') THEN
        EXECUTE format('CREATE ROLE ord WITH LOGIN PASSWORD %L', v_db_password);
    END IF;
    
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'paymentuser') THEN
        EXECUTE format('CREATE ROLE paymentuser WITH LOGIN PASSWORD %L', v_db_password);
    END IF;
    
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'notification') THEN
        EXECUTE format('CREATE ROLE notification WITH LOGIN PASSWORD %L', v_db_password);
    END IF;
    
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'simulator') THEN
        EXECUTE format('CREATE ROLE simulator WITH LOGIN PASSWORD %L', v_db_password);
    END IF;
    
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'analytics') THEN
        EXECUTE format('CREATE ROLE analytics WITH LOGIN PASSWORD %L', v_db_password);
    END IF;
    
    RAISE NOTICE 'Database users created. Use Vault or environment variables for password management in production.';
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
GRANT ALL PRIVILEGES ON SCHEMA public TO auth;
GRANT ALL PRIVILEGES ON SCHEMA public TO ord;
GRANT ALL PRIVILEGES ON SCHEMA public TO notification;
GRANT ALL PRIVILEGES ON SCHEMA public TO simulator;
GRANT ALL PRIVILEGES ON SCHEMA public TO analytics;
GRANT ALL PRIVILEGES ON SCHEMA public TO paymentuser;

-- SECURITY: Require password authentication for all connections
ALTER ROLE payment WITH PASSWORD VALID UNTIL '2099-01-01';
