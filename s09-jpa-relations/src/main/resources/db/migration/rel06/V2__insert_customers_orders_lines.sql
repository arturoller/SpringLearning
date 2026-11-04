-- Dane startowe lekcji Rel06EntityToDto. Hasła to PRZYKŁADOWE, testowe „skróty” — nie prawdziwe dane.
-- ZAM-001 (Anna, NEW, z uwagą): Klawiatura ×1 + Mysz ×2 = 267.90
-- ZAM-002 (Jan, SHIPPED):       Monitor ×1               = 899.99
-- ZAM-003 (Anna, NEW):          Kubek ×4                 =  98.00
INSERT INTO customer (name, email, password_hash) VALUES ('Anna Nowak', 'anna@sklep.pl', 'test-only-hash-anna');
INSERT INTO customer (name, email, password_hash) VALUES ('Jan Kowalski', 'jan@sklep.pl', 'test-only-hash-jan');

INSERT INTO product (name, price) VALUES ('Klawiatura', 149.90);
INSERT INTO product (name, price) VALUES ('Mysz',        59.00);
INSERT INTO product (name, price) VALUES ('Monitor',    899.99);
INSERT INTO product (name, price) VALUES ('Kubek',       24.50);

INSERT INTO orders (order_number, customer_id, status, note) VALUES ('ZAM-001', 1, 'NEW', 'Proszę dzwonić przed dostawą');
INSERT INTO orders (order_number, customer_id, status, note) VALUES ('ZAM-002', 2, 'SHIPPED', NULL);
INSERT INTO orders (order_number, customer_id, status, note) VALUES ('ZAM-003', 1, 'NEW', NULL);

INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (1, 1, 1, 149.90);
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (1, 2, 2,  59.00);
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (2, 3, 1, 899.99);
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (3, 4, 4,  24.50);
