-- Dane startowe lekcji Jpa04CustomQueries: dziesięć produktów (id 1–10) w trzech kategoriach (te same co w Jpa03).
-- Kategoria GARDEN celowo nie ma żadnego produktu; dwa produkty mają stan magazynu 0 (Lampka, Krzesło biurowe).
INSERT INTO product (name, category, price, stock, created_at) VALUES ('Klawiatura',      'COMPUTERS',  149.90,  12, TIMESTAMP WITH TIME ZONE '2026-09-01 10:00:00+00');
INSERT INTO product (name, category, price, stock, created_at) VALUES ('Mysz',            'COMPUTERS',   59.00,  30, TIMESTAMP WITH TIME ZONE '2026-09-02 10:00:00+00');
INSERT INTO product (name, category, price, stock, created_at) VALUES ('Monitor',         'COMPUTERS',  899.99,   5, TIMESTAMP WITH TIME ZONE '2026-09-03 10:00:00+00');
INSERT INTO product (name, category, price, stock, created_at) VALUES ('Laptop',          'COMPUTERS', 3499.00,   3, TIMESTAMP WITH TIME ZONE '2026-09-04 10:00:00+00');
INSERT INTO product (name, category, price, stock, created_at) VALUES ('Kubek',           'HOME',        24.50, 100, TIMESTAMP WITH TIME ZONE '2026-09-05 10:00:00+00');
INSERT INTO product (name, category, price, stock, created_at) VALUES ('Lampka',          'HOME',        79.00,   0, TIMESTAMP WITH TIME ZONE '2026-09-06 10:00:00+00');
INSERT INTO product (name, category, price, stock, created_at) VALUES ('Kubek termiczny', 'HOME',        59.00,  20, TIMESTAMP WITH TIME ZONE '2026-09-07 10:00:00+00');
INSERT INTO product (name, category, price, stock, created_at) VALUES ('Zeszyt',          'OFFICE',       4.50, 200, TIMESTAMP WITH TIME ZONE '2026-09-08 10:00:00+00');
INSERT INTO product (name, category, price, stock, created_at) VALUES ('Długopis',        'OFFICE',       2.99, 500, TIMESTAMP WITH TIME ZONE '2026-09-09 10:00:00+00');
INSERT INTO product (name, category, price, stock, created_at) VALUES ('Krzesło biurowe', 'OFFICE',     599.00,   0, TIMESTAMP WITH TIME ZONE '2026-09-10 10:00:00+00');
