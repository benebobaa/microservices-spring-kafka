DROP TABLE IF EXISTS order_items;

DROP TABLE IF EXISTS orders;

CREATE TABLE IF NOT EXISTS
    orders (
        id BIGSERIAL PRIMARY KEY,
        customer_id BIGINT NOT NULL,
        billing_address VARCHAR(255) NOT NULL,
        shipping_address VARCHAR(255),
        order_status VARCHAR(255),
        payment_method VARCHAR(255),
        total_amount NUMERIC(10, 2) NOT NULL,
        order_date DATE NOT NULL DEFAULT CURRENT_DATE
    );

CREATE TABLE IF NOT EXISTS
    order_items (
        id BIGSERIAL PRIMARY KEY,
        product_id BIGINT NOT NULL,
        price NUMERIC(10, 2) NOT NULL,
        quantity INTEGER NOT NULL,
        order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE
    );