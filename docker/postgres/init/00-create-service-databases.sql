SELECT 'CREATE DATABASE paymentdb'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'paymentdb')\gexec

SELECT 'CREATE DATABASE authdb'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'authdb')\gexec

SELECT 'CREATE DATABASE orderdb'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'orderdb')\gexec

SELECT 'CREATE DATABASE notificationdb'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'notificationdb')\gexec

SELECT 'CREATE DATABASE webhookdb'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'webhookdb')\gexec

SELECT 'CREATE DATABASE riskdb'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'riskdb')\gexec

SELECT 'CREATE DATABASE settlementdb'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'settlementdb')\gexec

SELECT 'CREATE DATABASE simulatordb'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'simulatordb')\gexec

SELECT 'CREATE DATABASE analyticsdb'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'analyticsdb')\gexec

SELECT 'CREATE DATABASE merchantdb'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'merchantdb')\gexec

SELECT 'CREATE DATABASE disputedb'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'disputedb')\gexec

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'payment') THEN
        CREATE ROLE payment WITH LOGIN PASSWORD 'paymentpass';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'auth') THEN
        CREATE ROLE auth WITH LOGIN PASSWORD 'authpass';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'ord') THEN
        CREATE ROLE ord WITH LOGIN PASSWORD 'orderpass';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'paymentuser') THEN
        CREATE ROLE paymentuser WITH LOGIN PASSWORD 'paymentpass';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'notification') THEN
        CREATE ROLE notification WITH LOGIN PASSWORD 'notificationpass';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'webhook') THEN
        CREATE ROLE webhook WITH LOGIN PASSWORD 'webhookpass';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'simulator') THEN
        CREATE ROLE simulator WITH LOGIN PASSWORD 'simulatorpass';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'settlement') THEN
        CREATE ROLE settlement WITH LOGIN PASSWORD 'settlementpass';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'risk') THEN
        CREATE ROLE risk WITH LOGIN PASSWORD 'riskpass';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'analytics') THEN
        CREATE ROLE analytics WITH LOGIN PASSWORD 'analyticspass';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'merchant') THEN
        CREATE ROLE merchant WITH LOGIN PASSWORD 'merchantpass';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'dispute') THEN
        CREATE ROLE dispute WITH LOGIN PASSWORD 'disputepass';
    END IF;
END
$$;

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
