node-pg facade
==============

docker run --name storm-postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres

PGPASSWORD=postgres psql -h localhost -U postgres -d postgres -p 5432

CREATE TABLE users (
id SERIAL PRIMARY KEY,
name TEXT NOT NULL,
email TEXT NOT NULL
);

INSERT INTO users (name, email) VALUES
('Alice', 'alice@test.com'),
('Bob', 'bob@test.com');
