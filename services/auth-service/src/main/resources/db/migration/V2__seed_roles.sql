-- Seed default roles
INSERT INTO roles (name, description) VALUES
    ('ROLE_ADMIN', 'System Administrator with full access'),
    ('ROLE_USER', 'Regular user with limited access')
ON CONFLICT (name) DO NOTHING;
