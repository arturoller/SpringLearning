-- Dane startowe lekcji Test04DataJpaTest: 2 kategorie (id 1-2) i 5 produktów (id 1-5).
INSERT INTO category (name) VALUES ('Komputery');
INSERT INTO category (name) VALUES ('Dom');

INSERT INTO product (name, category_id, price, stock) VALUES ('Klawiatura', 1, 149.90,  12);
INSERT INTO product (name, category_id, price, stock) VALUES ('Mysz',       1,  59.00,  30);
INSERT INTO product (name, category_id, price, stock) VALUES ('Monitor',    1, 899.99,   5);
INSERT INTO product (name, category_id, price, stock) VALUES ('Kubek',      2,  24.50, 100);
INSERT INTO product (name, category_id, price, stock) VALUES ('Lampka',     2,  79.00,   0);
