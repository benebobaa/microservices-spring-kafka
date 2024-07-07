DROP TABLE IF EXISTS customer_balance;
DROP TABLE IF EXISTS transaction_details;

CREATE TABLE IF NOT EXISTS
    transaction_details (
                            id BIGSERIAL PRIMARY KEY,
                            order_id BIGINT NOT NULL,
                            customer_id BIGINT NOT NULL,
                            amount DECIMAL(10, 2) NOT NULL,
                            mode VARCHAR(255) NOT NULL,
                            status VARCHAR(255) NOT NULL,
                            reference_number VARCHAR(255) NOT NULL,
                            payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS
    customer_balance (
                         id BIGSERIAL PRIMARY KEY,
                         customer_id BIGINT NOT NULL,
                         balance DECIMAL(10, 2) NOT NULL
);

INSERT INTO customer_balance (customer_id, balance) VALUES (1, 10000.00);
INSERT INTO customer_balance (customer_id, balance) VALUES (2, 0.00);