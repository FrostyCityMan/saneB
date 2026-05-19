-- local profile only: announcement input smoke operator account.
INSERT INTO users (
    id,
    login_id,
    password_hash,
    name,
    status_code,
    password_reset_required,
    created_at,
    updated_at
) VALUES (
    '10000000-0000-0000-0000-000000000002',
    'local_operator',
    '$2a$10$InQi9a3ehghCfxu2Z59DiegEEW4pfhxb4h19PCJb58D0/1OWmmQ2y',
    'Local Operator',
    'ACTIVE',
    false,
    now(),
    now()
)
ON CONFLICT (login_id) DO UPDATE
SET
    password_hash = EXCLUDED.password_hash,
    name = EXCLUDED.name,
    status_code = EXCLUDED.status_code,
    password_reset_required = EXCLUDED.password_reset_required,
    updated_at = now();

INSERT INTO user_roles (
    user_id,
    role_code,
    created_at
) VALUES (
    '10000000-0000-0000-0000-000000000002',
    'OPERATOR',
    now()
)
ON CONFLICT (user_id, role_code) DO NOTHING;
