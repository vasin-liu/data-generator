CREATE TABLE IF NOT EXISTS fixture_customers (
    id BIGINT PRIMARY KEY,
    name VARCHAR(64) NOT NULL
);
INSERT INTO fixture_customers (id, name) VALUES (1, 'alice'), (2, 'bob'), (3, 'carol');
