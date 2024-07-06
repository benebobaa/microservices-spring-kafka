DROP TABLE IF EXISTS products;

CREATE TABLE IF NOT EXISTS
    products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price NUMERIC(10, 2) NOT NULL,
    category VARCHAR(255) NOT NULL,
    description TEXT,
    image_url TEXT,
    stock_quantity INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO products (name, price, category, description, image_url, stock_quantity)
VALUES ('Ketoprak', 100.00, 'Category 1', 'Description 1', 'https://via.placeholder.com/150', 20),
       ('Nasi Goreng', 200.00, 'Category 2', 'Description 2', 'https://via.placeholder.com/150', 20),
       ('Nasi Uduk', 300.00, 'Category 2', 'Description 3', 'https://via.placeholder.com/150', 20);
