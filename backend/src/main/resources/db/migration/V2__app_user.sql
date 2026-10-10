CREATE TABLE app_user (
    id           BIGINT PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL
);

-- Fixed seed data. Users are never created at runtime (see ADR 0001).
INSERT INTO app_user (id, display_name) VALUES
    (1, 'Alice'),
    (2, 'Bob'),
    (3, 'Chloe'),
    (4, 'Dan');
