#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE payflow_auth;
    CREATE DATABASE payflow_order;
    CREATE DATABASE payflow_payment;
    CREATE DATABASE payflow_notification;
    CREATE DATABASE payflow_analytics;
    CREATE DATABASE payflow_audit;
EOSQL