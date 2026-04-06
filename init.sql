-- Payment Gateway Database - MariaDB
CREATE DATABASE IF NOT EXISTS payment_gateway;
USE payment_gateway;

-- Create user
CREATE USER IF NOT EXISTS 'payment'@'%' IDENTIFIED BY 'devpassword';
CREATE USER IF NOT EXISTS 'payment'@'localhost' IDENTIFIED BY 'devpassword';
GRANT ALL PRIVILEGES ON payment_gateway.* TO 'payment'@'%';
GRANT ALL PRIVILEGES ON payment_gateway.* TO 'payment'@'localhost';
FLUSH PRIVILEGES;

-- Users & Roles
CREATE TABLE IF NOT EXISTS roles (
    id BINARY(16) PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS users (
    id BINARY(16) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(120) NOT NULL,
    last_name VARCHAR(120) NOT NULL,
    phone VARCHAR(20),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BINARY(16) NOT NULL,
    role_id BINARY(16) NOT NULL,
    PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Orders
CREATE TABLE IF NOT EXISTS orders (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16),
    external_reference VARCHAR(100),
    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    customer_email VARCHAR(255),
    status VARCHAR(32) NOT NULL,
    description VARCHAR(255),
    merchant_id VARCHAR(100),
    metadata TEXT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Payments
CREATE TABLE IF NOT EXISTS payments (
    id BINARY(16) PRIMARY KEY,
    order_id BINARY(16),
    provider_order_id VARCHAR(64) NOT NULL UNIQUE,
    provider_payment_id VARCHAR(64),
    idempotency_key VARCHAR(120) NOT NULL UNIQUE,
    amount DECIMAL(19,2) NOT NULL,
    refunded_amount DECIMAL(19,2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    method VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    transaction_mode VARCHAR(16) NOT NULL,
    simulated BOOLEAN NOT NULL DEFAULT FALSE,
    provider_signature VARCHAR(255),
    checkout_url VARCHAR(255) NOT NULL,
    notes VARCHAR(255),
    merchant_id BINARY(16) NOT NULL,
    platform_fee DECIMAL(19,2) NOT NULL DEFAULT 0,
    gateway_fee DECIMAL(19,2) NOT NULL DEFAULT 0,
    pricing_tier VARCHAR(32) DEFAULT 'STANDARD',
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS transactions (
    id BINARY(16) PRIMARY KEY,
    payment_id BINARY(16) NOT NULL,
    type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    provider_reference VARCHAR(64) NOT NULL,
    remarks VARCHAR(255),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Simulator
CREATE TABLE IF NOT EXISTS simulation_transactions (
    id BINARY(16) PRIMARY KEY,
    order_reference VARCHAR(64) NOT NULL,
    payment_reference VARCHAR(64) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    provider_order_id VARCHAR(80) NOT NULL UNIQUE,
    provider_payment_id VARCHAR(80),
    provider_signature VARCHAR(255),
    simulation_mode VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    checkout_url VARCHAR(255) NOT NULL,
    webhook_callback_url VARCHAR(512),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Analytics
CREATE TABLE IF NOT EXISTS settlements (
    id BINARY(16) PRIMARY KEY,
    settlement_reference VARCHAR(50) NOT NULL UNIQUE,
    merchant_id BINARY(16) NOT NULL,
    merchant_name VARCHAR(255),
    total_transactions INT NOT NULL DEFAULT 0,
    total_amount DECIMAL(19,2) NOT NULL DEFAULT 0,
    total_fees DECIMAL(19,2) NOT NULL DEFAULT 0,
    total_refunds DECIMAL(19,2) NOT NULL DEFAULT 0,
    net_amount DECIMAL(19,2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'INR',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    period_start DATETIME(6) NOT NULL,
    period_end DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Default roles (using HEX for binary)
INSERT INTO roles (id, name, description) VALUES 
(UNHEX('11111111111111111111111111111111'), 'ADMIN', 'System Administrator'),
(UNHEX('22222222222222222222222222222222'), 'MERCHANT', 'Merchant User'),
(UNHEX('33333333333333333333333333333333'), 'CUSTOMER', 'Customer User')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- API Keys table
CREATE TABLE IF NOT EXISTS api_keys (
    id BINARY(16) PRIMARY KEY,
    merchant_id BINARY(16) NOT NULL,
    key_prefix VARCHAR(16) NOT NULL,
    key_hash VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(512),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    permissions TEXT,
    rate_limit_per_minute INT DEFAULT 100,
    rate_limit_per_day INT DEFAULT 10000,
    ip_whitelist TEXT,
    referer_whitelist TEXT,
    last_used_at DATETIME(6),
    last_used_ip VARCHAR(45),
    expires_at DATETIME(6),
    rotated_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Notification Service - Feature Flags
CREATE TABLE IF NOT EXISTS feature_flags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    key VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    rollout_percentage INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS feature_flag_user_ids (
    feature_flag_id BIGINT NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (feature_flag_id, user_id),
    FOREIGN KEY (feature_flag_id) REFERENCES feature_flags(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;