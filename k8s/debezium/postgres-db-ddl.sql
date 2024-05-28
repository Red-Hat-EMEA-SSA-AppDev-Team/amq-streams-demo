\connect debezium;

CREATE TABLE shipping (
  order_id INT NOT NULL PRIMARY KEY,
  shipping_method VARCHAR(50) NOT NULL,
  shipping_address VARCHAR(200) NOT NULL
);

INSERT INTO shipping (order_id, shipping_method, shipping_address)
VALUES (100, 'Standard', 'Main Street');

CREATE TABLE payments (
  payment_id SERIAL PRIMARY KEY,
  order_id INT,
  payment_method VARCHAR(50) NOT NULL,
  transaction_id VARCHAR(50),
  payment_amount DECIMAL(10,2) NOT NULL,
  payment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  payment_status VARCHAR(50) NOT NULL DEFAULT 'Pending'
);

