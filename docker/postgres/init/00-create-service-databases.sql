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

GRANT ALL PRIVILEGES ON DATABASE paymentdb TO payment;
GRANT ALL PRIVILEGES ON DATABASE authdb TO payment;
GRANT ALL PRIVILEGES ON DATABASE orderdb TO payment;
GRANT ALL PRIVILEGES ON DATABASE notificationdb TO payment;
GRANT ALL PRIVILEGES ON DATABASE webhookdb TO payment;
GRANT ALL PRIVILEGES ON DATABASE riskdb TO payment;
GRANT ALL PRIVILEGES ON DATABASE settlementdb TO payment;
GRANT ALL PRIVILEGES ON DATABASE simulatordb TO payment;
GRANT ALL PRIVILEGES ON DATABASE analyticsdb TO payment;
GRANT ALL PRIVILEGES ON DATABASE merchantdb TO payment;
GRANT ALL PRIVILEGES ON DATABASE disputedb TO payment;

ALTER DATABASE paymentdb OWNER TO payment;
ALTER DATABASE authdb OWNER TO payment;
ALTER DATABASE orderdb OWNER TO payment;
ALTER DATABASE notificationdb OWNER TO payment;
ALTER DATABASE webhookdb OWNER TO payment;
ALTER DATABASE riskdb OWNER TO payment;
ALTER DATABASE settlementdb OWNER TO payment;
ALTER DATABASE simulatordb OWNER TO payment;
ALTER DATABASE analyticsdb OWNER TO payment;
ALTER DATABASE merchantdb OWNER TO payment;
ALTER DATABASE disputedb OWNER TO payment;