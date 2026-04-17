-- Auth Service Baseline
-- V1__baseline.sql

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    password VARCHAR(255),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    phone VARCHAR(20),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);

-- Roles table
CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE INDEX idx_roles_name ON roles(name);

-- User-Role mapping table
CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Default roles
INSERT INTO roles (id, name, description) VALUES 
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'ROLE_USER', 'Regular user'),
    ('b0eec111-9c1c-4ef9-bb7d-6cc0bd380a22', 'ROLE_ADMIN', 'Administrator'),
    ('c0eed333-9d2d-4ef0-bb8e-6dd0cd380a33', 'ROLE_MERCHANT', 'Merchant user')
ON CONFLICT DO NOTHING;