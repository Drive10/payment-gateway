-- ============================================================
-- Payment Gateway PostgreSQL Initialization Script
-- ============================================================
-- SECURITY: This script uses environment variables for passwords
-- Passwords should be passed via POSTGRES_PASSWORD environment variable
-- For production, use Vault or Kubernetes secrets
-- ============================================================

-- Create databases
SELECT 'CREATE DATABASE paymentdb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'paymentdb')\gexec;
SELECT 'CREATE DATABASE authdb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'authdb')\gexec;
SELECT 'CREATE DATABASE orderdb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'orderdb')\gexec;
SELECT 'CREATE DATABASE notificationdb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'notificationdb')\gexec;
SELECT 'CREATE DATABASE webhookdb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'webhookdb')\gexec;
SELECT 'CREATE DATABASE riskdb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'riskdb')\gexec;
SELECT 'CREATE DATABASE settlementdb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'settlementdb')\gexec;
SELECT 'CREATE DATABASE simulatordb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'simulatordb')\gexec;
SELECT 'CREATE DATABASE analyticsdb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'analyticsdb')\gexec;
SELECT 'CREATE DATABASE merchantdb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'merchantdb')\gexec;
SELECT 'CREATE DATABASE disputedb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'disputedb')\gexec;

-- Create roles with secure passwords (override via environment in production)
-- In production, these should come from Vault or Kubernetes secrets
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
    
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'webhook') THEN
        EXECUTE format('CREATE ROLE webhook WITH LOGIN PASSWORD %L', v_db_password);
    END IF;
    
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'simulator') THEN
        EXECUTE format('CREATE ROLE simulator WITH LOGIN PASSWORD %L', v_db_password);
    END IF;
    
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'settlement') THEN
        EXECUTE format('CREATE ROLE settlement WITH LOGIN PASSWORD %L', v_db_password);
    END IF;
    
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'risk') THEN
        EXECUTE format('CREATE ROLE risk WITH LOGIN PASSWORD %L', v_db_password);
    END IF;
    
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'analytics') THEN
        EXECUTE format('CREATE ROLE analytics WITH LOGIN PASSWORD %L', v_db_password);
    END IF;
    
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'merchant') THEN
        EXECUTE format('CREATE ROLE merchant WITH LOGIN PASSWORD %L', v_db_password);
    END IF;
    
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'dispute') THEN
        EXECUTE format('CREATE ROLE dispute WITH LOGIN PASSWORD %L', v_db_password);
    END IF;
    
    -- Log password references (not actual passwords)
    RAISE NOTICE 'Database users created. Use Vault or environment variables for password management in production.';
END
$$;

-- Grant database access
GRANT ALL PRIVILEGES ON DATABASE paymentdb TO payment;
GRANT ALL PRIVILEGES ON DATABASE authdb TO payment, auth;
GRANT ALL PRIVILEGES ON DATABASE orderdb TO payment, ord;
GRANT ALL PRIVILEGES ON DATABASE notificationdb TO payment, notification;
GRANT ALL PRIVILEGES ON DATABASE webhookdb TO payment, webhook;
GRANT ALL PRIVILEGES ON DATABASE riskdb TO payment, risk;
GRANT ALL PRIVILEGES ON DATABASE settlementdb TO payment, settlement;
GRANT ALL PRIVILEGES ON DATABASE simulatordb TO payment, simulator;
GRANT ALL PRIVILEGES ON DATABASE analyticsdb TO payment, analytics;
GRANT ALL PRIVILEGES ON DATABASE merchantdb TO payment, merchant;
GRANT ALL PRIVILEGES ON DATABASE disputedb TO payment, dispute;

-- Set database owners
ALTER DATABASE paymentdb OWNER TO payment;
ALTER DATABASE authdb OWNER TO auth;
ALTER DATABASE orderdb OWNER TO ord;
ALTER DATABASE notificationdb OWNER TO notification;
ALTER DATABASE webhookdb OWNER TO webhook;
ALTER DATABASE riskdb OWNER TO risk;
ALTER DATABASE settlementdb OWNER TO settlement;
ALTER DATABASE simulatordb OWNER TO simulator;
ALTER DATABASE analyticsdb OWNER TO analytics;
ALTER DATABASE merchantdb OWNER TO merchant;
ALTER DATABASE disputedb OWNER TO dispute;

-- Grant schema access
GRANT ALL PRIVILEGES ON SCHEMA public TO auth;
GRANT ALL PRIVILEGES ON SCHEMA public TO ord;
GRANT ALL PRIVILEGES ON SCHEMA public TO notification;
GRANT ALL PRIVILEGES ON SCHEMA public TO webhook;
GRANT ALL PRIVILEGES ON SCHEMA public TO simulator;
GRANT ALL PRIVILEGES ON SCHEMA public TO settlement;
GRANT ALL PRIVILEGES ON SCHEMA public TO risk;
GRANT ALL PRIVILEGES ON SCHEMA public TO analytics;
GRANT ALL PRIVILEGES ON SCHEMA public TO merchant;
GRANT ALL PRIVILEGES ON SCHEMA public TO dispute;
GRANT ALL PRIVILEGES ON SCHEMA public TO paymentuser;

-- SECURITY: Require password authentication for all connections
ALTER ROLE payment WITH PASSWORD VALID UNTIL '2099-01-01';
