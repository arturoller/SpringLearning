-- Dane startowe lekcji Rel05NPlusOne: 4 klientów, 5 produktów, 10 zamówień, 19 pozycji, 11 płatności.
-- Klienci zamówień: Anna (1) — ZAM-001, 002, 006, 010; Jan (2) — 003, 007; Ewa (3) — 004, 008; Piotr (4) — 005, 009.
-- Pozycje: każde zamówienie ma 2 pozycje, ZAM-003 ma 3, a ZAM-010 jest PUSTE (bez pozycji i bez płatności).
-- Płatności: po jednej na zamówienie (kwota = wartość zamówienia), ZAM-002 i ZAM-005 płacone w dwóch ratach.
INSERT INTO customer (name, email) VALUES ('Anna Nowak', 'anna@sklep.pl');
INSERT INTO customer (name, email) VALUES ('Jan Kowalski', 'jan@sklep.pl');
INSERT INTO customer (name, email) VALUES ('Ewa Wiśniewska', 'ewa@sklep.pl');
INSERT INTO customer (name, email) VALUES ('Piotr Zieliński', 'piotr@sklep.pl');

INSERT INTO product (name, price) VALUES ('Klawiatura', 149.90);
INSERT INTO product (name, price) VALUES ('Mysz',        59.00);
INSERT INTO product (name, price) VALUES ('Monitor',    899.99);
INSERT INTO product (name, price) VALUES ('Kubek',       24.50);
INSERT INTO product (name, price) VALUES ('Słuchawki',  199.00);

INSERT INTO orders (order_number, customer_id) VALUES ('ZAM-001', 1);
INSERT INTO orders (order_number, customer_id) VALUES ('ZAM-002', 1);
INSERT INTO orders (order_number, customer_id) VALUES ('ZAM-003', 2);
INSERT INTO orders (order_number, customer_id) VALUES ('ZAM-004', 3);
INSERT INTO orders (order_number, customer_id) VALUES ('ZAM-005', 4);
INSERT INTO orders (order_number, customer_id) VALUES ('ZAM-006', 1);
INSERT INTO orders (order_number, customer_id) VALUES ('ZAM-007', 2);
INSERT INTO orders (order_number, customer_id) VALUES ('ZAM-008', 3);
INSERT INTO orders (order_number, customer_id) VALUES ('ZAM-009', 4);
INSERT INTO orders (order_number, customer_id) VALUES ('ZAM-010', 1);

-- ZAM-001 = 149.90 + 2×59.00 = 267.90
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (1, 1, 1, 149.90);
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (1, 2, 2,  59.00);
-- ZAM-002 = 899.99 + 24.50 = 924.49
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (2, 3, 1, 899.99);
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (2, 4, 1,  24.50);
-- ZAM-003 = 4×24.50 + 59.00 + 199.00 = 356.00
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (3, 4, 4,  24.50);
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (3, 2, 1,  59.00);
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (3, 5, 1, 199.00);
-- ZAM-004 = 199.00 + 149.90 = 348.90
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (4, 5, 1, 199.00);
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (4, 1, 1, 149.90);
-- ZAM-005 = 2×899.99 + 2×24.50 = 1848.98
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (5, 3, 2, 899.99);
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (5, 4, 2,  24.50);
-- ZAM-006 = 59.00 + 199.00 = 258.00
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (6, 2, 1,  59.00);
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (6, 5, 1, 199.00);
-- ZAM-007 = 149.90 + 24.50 = 174.40
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (7, 1, 1, 149.90);
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (7, 4, 1,  24.50);
-- ZAM-008 = 899.99 + 59.00 = 958.99
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (8, 3, 1, 899.99);
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (8, 2, 1,  59.00);
-- ZAM-009 = 3×24.50 + 199.00 = 272.50
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (9, 4, 3,  24.50);
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (9, 5, 1, 199.00);

INSERT INTO payment (order_id, amount) VALUES (1,  267.90);
INSERT INTO payment (order_id, amount) VALUES (2,  500.00);
INSERT INTO payment (order_id, amount) VALUES (2,  424.49);
INSERT INTO payment (order_id, amount) VALUES (3,  356.00);
INSERT INTO payment (order_id, amount) VALUES (4,  348.90);
INSERT INTO payment (order_id, amount) VALUES (5, 1000.00);
INSERT INTO payment (order_id, amount) VALUES (5,  848.98);
INSERT INTO payment (order_id, amount) VALUES (6,  258.00);
INSERT INTO payment (order_id, amount) VALUES (7,  174.40);
INSERT INTO payment (order_id, amount) VALUES (8,  958.99);
INSERT INTO payment (order_id, amount) VALUES (9,  272.50);
