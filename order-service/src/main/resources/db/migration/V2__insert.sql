
INSERT INTO orders (customer_id, billing_address, shipping_address, order_status, payment_method, total_amount, order_date)
VALUES
    (1, '1234 Market St', '1234 Market St', 'CREATED', 'BRI', 100.00, '2023-01-01'),
    (2, '5678 Elm St', '5678 Elm St', 'COMPLETED', 'BCA', 200.50, '2023-01-02'),
    (3, '91011 Pine St', NULL, 'CANCELLED', 'GOPAY', 150.75, '2023-01-03');

INSERT INTO order_items (product_id, price, quantity, order_id)
VALUES
    (1, 50.00, 2, 1),
    (2, 200.50, 1, 2),
    (3, 75.25, 2, 3),
    (1, 25.00, 3, 3);