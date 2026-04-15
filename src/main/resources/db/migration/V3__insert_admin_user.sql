INSERT INTO users (id, name, email, password, role, active, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'Administrator',
    'admin@auth.com',
    '$2a$12$LqOECFCzGkLRLOPUH16hfOJmjU0R.1gIyLfqIKCqnMdVJP5DnLkiO',
    'ADMIN',
    true,
    NOW(),
    NOW()
);
