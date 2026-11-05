-- Dane startowe lekcji Rel03ManyToMany.
-- Produkty (id 1–5) i ich tagi:            Tagi (id 1–5) i liczba produktów:
--   1 Klawiatura: biuro, gaming              1 biuro          3 (Klawiatura, Mysz, Lampka)
--   2 Mysz:       biuro, gaming,             2 gaming         3 (Klawiatura, Mysz, Monitor)
--                 bezprzewodowe, promocja    3 promocja       2 (Mysz, Kubek)
--   3 Monitor:    gaming                     4 bezprzewodowe  1 (Mysz)
--   4 Kubek:      promocja                   5 nowość         0
--   5 Lampka:     biuro
INSERT INTO product (name, price) VALUES ('Klawiatura', 149.90);
INSERT INTO product (name, price) VALUES ('Mysz',        59.00);
INSERT INTO product (name, price) VALUES ('Monitor',    899.99);
INSERT INTO product (name, price) VALUES ('Kubek',       24.50);
INSERT INTO product (name, price) VALUES ('Lampka',      79.00);

INSERT INTO tag (name) VALUES ('biuro');
INSERT INTO tag (name) VALUES ('gaming');
INSERT INTO tag (name) VALUES ('promocja');
INSERT INTO tag (name) VALUES ('bezprzewodowe');
INSERT INTO tag (name) VALUES ('nowość');

INSERT INTO product_tag (product_id, tag_id) VALUES (1, 1);
INSERT INTO product_tag (product_id, tag_id) VALUES (1, 2);
INSERT INTO product_tag (product_id, tag_id) VALUES (2, 1);
INSERT INTO product_tag (product_id, tag_id) VALUES (2, 2);
INSERT INTO product_tag (product_id, tag_id) VALUES (2, 3);
INSERT INTO product_tag (product_id, tag_id) VALUES (2, 4);
INSERT INTO product_tag (product_id, tag_id) VALUES (3, 2);
INSERT INTO product_tag (product_id, tag_id) VALUES (4, 3);
INSERT INTO product_tag (product_id, tag_id) VALUES (5, 1);

-- Dostawcy (id 1–2) i ceny zakupu: Klawiaturę dostarczają obaj (Beta taniej), Mysz tylko Alfa, reszty nikt.
INSERT INTO supplier (name) VALUES ('Hurtownia Alfa');
INSERT INTO supplier (name) VALUES ('Hurtownia Beta');

INSERT INTO product_supplier (product_id, supplier_id, purchase_price) VALUES (1, 1, 110.00);
INSERT INTO product_supplier (product_id, supplier_id, purchase_price) VALUES (1, 2, 105.00);
INSERT INTO product_supplier (product_id, supplier_id, purchase_price) VALUES (2, 1,  40.00);
