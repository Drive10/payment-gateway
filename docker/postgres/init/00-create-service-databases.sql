SELECT 'CREATE DATABASE paymentdb'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'paymentdb')\gexec

SELECT 'CREATE DATABASE authdb'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'authdb')\gexec

SELECT 'CREATE DATABASE ledgerdb'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ledgerdb')\gexec

SELECT 'CREATE DATABASE notificationdb'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'notificationdb')\gexec

SELECT 'CREATE DATABASE riskdb'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'riskdb')\gexec

SELECT 'CREATE DATABASE settlementdb'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'settlementdb')\gexec

SELECT 'CREATE DATABASE simulatordb'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'simulatordb')\gexec

SELECT 'CREATE DATABASE analyticsdb'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'analyticsdb')\gexec

CREATE USER settlement WITH PASSWORD 'settlementpass';
GRANT ALL PRIVILEGES ON DATABASE settlementdb TO settlement;

CREATE USER risk WITH PASSWORD 'riskpass';
GRANT ALL PRIVILEGES ON DATABASE riskdb TO risk;

CREATE USER analytics WITH PASSWORD 'analyticspass';
GRANT ALL PRIVILEGES ON DATABASE analyticsdb TO analytics;
