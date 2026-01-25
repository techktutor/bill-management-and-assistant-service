INSERT INTO customers (
    id,
    customer_type,
    full_name,
    email,
    status,
    version,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'INDIVIDUAL',
    'Test Customer',
    'test@example.com',
    'ACTIVE',
    0,
    now(),
    now()
);
