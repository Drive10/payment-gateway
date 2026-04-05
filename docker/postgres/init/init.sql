-- Payment Gateway Database Initialization Script
-- This file is executed automatically when PostgreSQL container starts
-- Creates databases and users only. Tables are created by Hibernate.

-- Create Databases
CREATE DATABASE authdb;
CREATE DATABASE orderdb;
CREATE DATABASE paymentdb;
CREATE DATABASE notificationdb;
CREATE DATABASE simulatordb;
CREATE DATABASE analyticsdb;

-- Create Users
CREATE USER auth WITH PASSWORD 'devpassword';
CREATE USER ord WITH PASSWORD 'devpassword';
CREATE USER paymentuser WITH PASSWORD 'devpassword';
CREATE USER notification WITH PASSWORD 'devpassword';
CREATE USER simulator WITH PASSWORD 'devpassword';
CREATE USER analytics WITH PASSWORD 'devpassword';

-- Grant Database Privileges
GRANT ALL PRIVILEGES ON DATABASE authdb TO auth;
GRANT ALL PRIVILEGES ON DATABASE orderdb TO ord;
GRANT ALL PRIVILEGES ON DATABASE paymentdb TO paymentuser;
GRANT ALL PRIVILEGES ON DATABASE notificationdb TO notification;
GRANT ALL PRIVILEGES ON DATABASE simulatordb TO simulator;
GRANT ALL PRIVILEGES ON DATABASE analyticsdb TO analytics;

-- Grant Schema Privileges
\c authdb;
GRANT ALL PRIVILEGES ON SCHEMA public TO auth;
ALTER USER auth CREATEDB;

\c orderdb;
GRANT ALL PRIVILEGES ON SCHEMA public TO ord;

\c paymentdb;
GRANT ALL PRIVILEGES ON SCHEMA public TO paymentuser;

\c notificationdb;
GRANT ALL PRIVILEGES ON SCHEMA public TO notification;

\c simulatordb;
GRANT ALL PRIVILEGES ON SCHEMA public TO simulator;

\c analyticsdb;
GRANT ALL PRIVILEGES ON SCHEMA public TO analytics;

\echo ''
\echo '==========================================='
\echo 'Payment Gateway Database Initialization Complete'
\echo '==========================================='
\echo 'Databases: authdb, orderdb, paymentdb, notificationdb, simulatordb, analyticsdb'
\echo 'Users: auth, ord, paymentuser, notification, simulator, analytics'
\echo 'Password: devpassword'
\echo 'Tables will be created by Hibernate (ddl-auto: update)'
\echo '==========================================='