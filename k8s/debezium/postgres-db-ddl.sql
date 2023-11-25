\connect debezium;
CREATE TABLE customers (
  id SERIAL PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  email VARCHAR(100) UNIQUE NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO customers (name, email)
VALUES ('John Doe', 'johndoe@example.com'),
       ('Jane Doe', 'janedoe@example.com'),
       ('Peter Jones', 'peterjones@example.com');