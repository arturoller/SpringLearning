-- Dane startowe lekcji Rel04LazyLoading.
-- Klienci: 1 Anna Nowak (ZAM-001, ZAM-002), 2 Jan Kowalski (ZAM-003), 3 Ewa Wiśniewska (bez zamówień).
-- ZAM-001: Klawiatura ×1, Mysz ×2;  ZAM-002: Monitor ×1;  ZAM-003: Kubek ×4, Mysz ×1.
INSERT INTO customer (name, email) VALUES ('Anna Nowak', 'anna@sklep.pl');
INSERT INTO customer (name, email) VALUES ('Jan Kowalski', 'jan@sklep.pl');
INSERT INTO customer (name, email) VALUES ('Ewa Wiśniewska', 'ewa@sklep.pl');

INSERT INTO product (name, price) VALUES ('Klawiatura', 149.90);
INSERT INTO product (name, price) VALUES ('Mysz',        59.00);
INSERT INTO product (name, price) VALUES ('Monitor',    899.99);
INSERT INTO product (name, price) VALUES ('Kubek',       24.50);

INSERT INTO orders (order_number, customer_id) VALUES ('ZAM-001', 1);
INSERT INTO orders (order_number, customer_id) VALUES ('ZAM-002', 1);
INSERT INTO orders (order_number, customer_id) VALUES ('ZAM-003', 2);

INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (1, 1, 1, 149.90);
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (1, 2, 2,  59.00);
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (2, 3, 1, 899.99);
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (3, 4, 4,  24.50);
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (3, 2, 1,  59.00);
