#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_USER" <<-EOSQL
    -- Create databases for each service
    CREATE DATABASE payflow_auth;
    CREATE DATABASE payflow_order;
    CREATE DATABASE payflow_payment;
    CREATE DATABASE payflow_notification;
    CREATE DATABASE payflow_analytics;
    CREATE DATABASE payflow_simulator;

    -- Grant privileges (optional since same user)
    ALTER DATABASE payflow_auth OWNER TO payflow;
    ALTER DATABASE payflow_order OWNER TO payflow;
    ALTER DATABASE payflow_payment OWNER TO payflow;
    ALTER DATABASE payflow_notification OWNER TO payflow;
    ALTER DATABASE payflow_analytics OWNER TO payflow;
    ALTER DATABASE payflow_simulator OWNER TO payflow;
EOSQL

echo "Databases created successfully"