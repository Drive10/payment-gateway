INSERT INTO roles (name)
VALUES ('ADMIN'),
       ('USER')
ON CONFLICT (name) DO NOTHING;

INSERT INTO users (email, full_name, password, active)
VALUES ('admin@fintech.local', 'Platform Admin', '$2y$10$7B/5kEe/pB6xiJIw4eHiqeVvmj5SMkSgqQ7VniBO/FxjkwnZgzJyu', TRUE)
ON CONFLICT (email) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
         JOIN roles r ON r.name = 'ADMIN'
WHERE u.email = 'admin@fintech.local'
ON CONFLICT DO NOTHING;
