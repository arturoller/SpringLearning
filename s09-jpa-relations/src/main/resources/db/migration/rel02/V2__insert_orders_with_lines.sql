-- Dane startowe lekcji Rel02OneToMany: 4 produkty (id 1–4), 3 zamówienia (id 1–3), 5 pozycji (id 1–5).
-- ZAM-001: Klawiatura ×1 + Mysz ×2            = 149.90 + 118.00          =  267.90
-- ZAM-002: Monitor ×1 + Mysz ×1 + Kubek ×4    = 899.99 +  59.00 + 98.00  = 1056.99
-- ZAM-003: celowo PUSTE (bez pozycji).
INSERT INTO product (name, price) VALUES ('Klawiatura', 149.90);
INSERT INTO product (name, price) VALUES ('Mysz',        59.00);
INSERT INTO product (name, price) VALUES ('Monitor',    899.99);
INSERT INTO product (name, price) VALUES ('Kubek',       24.50);

INSERT INTO orders (order_number) VALUES ('ZAM-001');
INSERT INTO orders (order_number) VALUES ('ZAM-002');
INSERT INTO orders (order_number) VALUES ('ZAM-003');

INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (1, 1, 1, 149.90);
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (1, 2, 2,  59.00);
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (2, 3, 1, 899.99);
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (2, 2, 1,  59.00);
INSERT INTO order_line (order_id, product_id, quantity, unit_price) VALUES (2, 4, 4,  24.50);
