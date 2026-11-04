-- Dane startowe lekcji Jdbc02JdbcTemplate.
INSERT INTO product (name, category, price, stock) VALUES ('Mysz', 'komputery', 59.00, 30);
INSERT INTO product (name, category, price, stock) VALUES ('Monitor', 'komputery', 899.99, 5);
INSERT INTO product (name, category, price, stock) VALUES ('Kubek', 'dom', 24.50, 100);
INSERT INTO product (name, category, price, stock) VALUES ('Lampka', 'dom', 79.00, 0);

INSERT INTO customer (name, email) VALUES ('Anna Nowak', 'anna@example.com');
INSERT INTO customer (name, email) VALUES ('Jan Kowalski', 'jan@example.com');

INSERT INTO orders (customer_id, order_date) VALUES (1, DATE '2026-03-01');
INSERT INTO orders (customer_id, order_date) VALUES (2, DATE '2026-03-02');
INSERT INTO orders (customer_id, order_date) VALUES (1, DATE '2026-03-05');

INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (1, 1, 2, 59.00);
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (1, 3, 1, 24.50);
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (2, 2, 1, 899.99);
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (3, 3, 4, 24.50);
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (3, 1, 1, 59.00);
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (3, 4, 2, 79.00);
