CREATE TABLE IF NOT EXISTS fixture_orders (
    id BIGINT PRIMARY KEY,
    amount DECIMAL(10, 2) NOT NULL
);
INSERT INTO fixture_orders (id, amount) VALUES (1, 10.00), (2, 25.50), (3, 4.50);
